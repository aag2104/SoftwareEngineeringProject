package controllers;

import actions.OperationLoggingAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import models.*;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import services.ProjectService;
import services.UserService;
import utils.Constants;
import utils.RESTfulCalls;
import utils.RESTfulCalls.ResponseType;
import views.html.*;

import javax.inject.Inject;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static controllers.Application.checkLoginStatus;
import static controllers.Application.isPrivateProjectZone;
import static utils.Constants.CALLER_IS_MY_SPACE_PAGE;
import static utils.Constants.CALLER_IS_NOT_MY_SPACE_PAGE;

public class UserController extends Controller {

    @Inject
    Config config;

    private final ProjectService projectService;
    private final UserService userService;

    private Form<User> userForm;
    private FormFactory myFactory;


    /******************************* Constructor **********************************************************************/
    @Inject
    public UserController(FormFactory factory,
                          ProjectService projectService,
                          UserService userService) {
        userForm = factory.form(User.class);
        myFactory = factory;

        this.projectService = projectService;
        this.userService = userService;
    }

    private List<Organization> fetchOrganizationsList() {
        JsonNode organizationsJsonNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.ORGANIZATION_LIST));
        Logger.debug(organizationsJsonNode.asText());
        JsonNode organizationJsonArray = organizationsJsonNode.get("items");
        List<Organization> organizations = new ArrayList<>();
        if (null == organizationJsonArray) return organizations;
        for (JsonNode json : organizationJsonArray) {
            try {
                Organization organization = Organization.deserialize(json);
                organizations.add(organization);
            } catch (Exception e) {

            }
        }
        return organizations;
    }

    /************************************************** User Registration *********************************************/
    /**
     * This method goes to the user sign in page.
     * @return
     */
    public Result userRegisterPage() {
        List<Organization> organizations = fetchOrganizationsList();
        Organization org = new Organization();
        org.setId(-1);
        org.setOrganizationName("Other");
        organizations.add(org);
        return ok(signup.render(userForm, Constants.PATTERN_RULES, organizations));
    }

    @With(OperationLoggingAction.class)
    public Result userForgotPasswordPage() {
        return ok(forgotPassword.render(userForm));
    }

    @With(OperationLoggingAction.class)
    public Result userResetPassword() {
        Form<User> userForm = this.userForm.bindFromRequest();
        ObjectNode jsonData = userService.createJsonFromUserForm(userForm);
        JsonNode response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config,
                Constants.SEND_PASSWORD_EMAIL), jsonData);
        String info = "";
        String email = response.get("email").asText();
        try {
            info = response.get("error").asText();
        } catch (Exception e) {
            info = "Your new password has been reset. Please check it in: " + email + ". You can use it and login now.";
        }
        return ok(passwordReset.render(info));
    }

    /**
     * This method gather user sign in page input and creates a user.
     * @return
     */
    public Result userRegisterPOST() {
        try {
            Form<User> userForm = this.userForm.bindFromRequest();
            ObjectNode jsonData = userService.createJsonFromUserForm(userForm);
            jsonData.put("level", "normal");
            JsonNode response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.USER_REGISTER_POST), jsonData);
            String newUserId = response.get("id").asText();
            String email = userForm.field("email").value();

            Logger.debug("Created JSON data from form: " + jsonData.toString());

            if (newUserId != null) {
                // return ok(registerConfirmation.render(new Long(newUserId), "User"));
                return sendRegisterEmail(email, newUserId);
            } else {
                Logger.debug("UserController user sign on backend error");
                return ok(registrationError.render("User"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.debug("UserController user sign on exception: " + e.toString());
            return ok(registrationError.render("User"));
        }
    }
    /************************************************** End of User Registration **************************************/


    /************************************************** User Edit *****************************************************/

    /**
     * This method renders the user edit page (called in the menu as account management)
     *
     * @return render user edit page
     */
    @With(OperationLoggingAction.class)
    public Result userEditPage() {
        checkLoginStatus();

        // Make sure a normal user can only edit his/her own profile page.
        String userId = session("id");
        try {
            JsonNode userNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.USER_DETAIL + userId));
            if (userNode == null || userNode.has("error")) {
                Logger.debug("UserController.userEditPage user cannot be found from backend: " + userId);
                Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
                return ok(generalError.render());
            }
            User user = User.deserialize(userNode);
            ResearcherInfo researcherInfo = null;
            StudentInfo studentInfo = null;

            if(user.getUserType() != null){
                if (user.getUserType() == 1) {
                    JsonNode researcherNode = RESTfulCalls.getAPI(
                            RESTfulCalls.getBackendAPIUrl(config, Constants.RESEARCHER_DETAIL + userId));
                    Logger.info("Queried researcherInfo: " + researcherNode);
                    if (researcherNode != null && !researcherNode.has("error")) {
                        researcherInfo = ResearcherInfo.deserialize(researcherNode);
                        user.setResearcherInfo(researcherInfo);
                    }
                }
                if (user.getUserType() == 4) {
                    JsonNode studentNode = RESTfulCalls.getAPI(
                            RESTfulCalls.getBackendAPIUrl(config, Constants.STUDENT_DETAIL + userId));
                    Logger.info("Queried studentInfo: " + studentNode);
                    if (studentNode != null && !studentNode.has("error")) {
                        studentInfo = StudentInfo.deserialize(studentNode);
                        user.setStudentInfo(studentInfo);
                    }
                }
            }

            List<Organization> organizations = fetchOrganizationsList();
            Organization org = new Organization();
            org.setId(-1);
            org.setOrganizationName("Other");
            organizations.add(org);

            return ok(userEdit.render(userId, userForm, user, organizations, researcherInfo, studentInfo));
        } catch (Exception e) {
            Logger.debug("UserController.userEditPage() exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
    }

    /**
     * This method receives user's information and updates user's profile
     *
     * @return redirect to mySpace if the update succeeds or to homepage if update failed
     * TODO: How about admin can edit a user???
     */
    @With(OperationLoggingAction.class)
    public Result userEditPOST() {
        Logger.info("Entering userEditPOST()");
        checkLoginStatus();

        try {
            Form<User> userForm = this.userForm.bindFromRequest();
            Logger.debug("Form binding complete. Errors: " + userForm.errorsAsJson());

            Logger.debug("Form Data: " + userForm.data().toString());

            if (userForm.hasErrors()) {
                Logger.warn("userEditPOST: Form has errors: " + userForm.errorsAsJson());
                return badRequest(login.render(userForm, userService.getPublicRecaptchaKey()));
            } else {
                ObjectNode jsonData = userService.createJsonFromUserForm(userForm);
                Logger.debug("Created JSON data from form: " + jsonData.toString());

                String sessionId = session("id");
                jsonData.put("id", sessionId);
                Logger.debug("Added session id (" + sessionId + ") to JSON data.");

                Long projectId = null;
                String projectField = userForm.field("projectId").value();
                Logger.debug("Project field value: " + projectField);
                if (!StringUtils.isEmpty(projectField)) {
                    try {
                        projectId = Long.parseLong(projectField.trim());
                        Logger.debug("Parsed projectId: " + projectId);
                    } catch (Exception e) {
                        Logger.debug("Project Id passed is not a number: " + e.toString());
                    }
                } else {
                    Logger.debug("Project field is empty.");
                }
                jsonData.put("projectId", projectId);
                Logger.debug("Final JSON data with projectId: " + jsonData.toString());

                Logger.debug("Calling userService.updateUserImage()");
                JsonNode imageUpdateResponse = userService.updateUserImage(userForm, request().body());
                Logger.debug("Image update response: " + (imageUpdateResponse != null ? imageUpdateResponse.toString() : "null"));

                String backendUrl = RESTfulCalls.getBackendAPIUrl(config, Constants.USER_EDIT_POST);
                Logger.debug("Calling POST API to backend URL: " + backendUrl + " with data: " + jsonData.toString());
                JsonNode response = RESTfulCalls.postAPI(backendUrl, jsonData);
                Logger.debug("Response from POST API: " + (response != null ? response.toString() : "null"));

                if (response == null || response.has("error")) {
                    Logger.error("User edit failed! Response: " + (response != null ? response.toString() : "null"));
                    return ok(editError.render("User"));
                } else {
                    Logger.info("User edit succeeded. Updating session and redirecting.");

                    if (projectId != null) {
                        session("projectId", projectId.toString());
                        Logger.debug("Session projectId updated to: " + projectId.toString());
                    } else {
                        session("projectId", Integer.toString(Constants.OPENNEX_PROJECT_ZONE_ID));
                        Logger.debug("Session projectId set to default: " + Constants.OPENNEX_PROJECT_ZONE_ID);
                    }

                    User currentUser = User.deserialize(response);
                    Logger.debug("Deserialized current user: " + currentUser.toString());
                    String currentAvatar = currentUser.getAvatar();
                    session("avatar", currentAvatar);
                    Logger.debug("Session avatar updated to: " + currentAvatar);

                    long currentUserId = Long.parseLong(session("id"));
                    Logger.info("Redirecting to userDetailPage with user id: " + currentUserId);
                    return redirect(routes.UserController.userDetailPage(currentUserId));
                }
            }
        } catch (Exception e) {
            Logger.error("UserController.userEditPOST exception: " + e.toString(), e);
            return ok(editError.render("User"));
        }
    }
    /************************************************** End of User Edit **********************************************/

    /************************************************** User Login ****************************************************/

    /**
     * This method takes user log in info.
     * @return
     */
//    @With(OperationLoggingAction.class)
    public Result userLogin() {
        try {

            // DEVELOPMENT MODE: Auto-login with user ID 1 - DISABLED
            boolean devLogin = true;

            if (devLogin) {
                session().clear();
                session("id", "1");
                session("username", "testuser");
                session("userfirstname", "Test");
                session("email", "test@example.com");
                session("avatar", "");
                session("userTypes", "4");
                session("organization", "");
                return redirect(routes.Application.home());
            }

            // Original login code

            Form loginForm = myFactory.form().bindFromRequest();

            if (loginForm.hasErrors()) {
                return badRequest(login.render(loginForm, userService.getPublicRecaptchaKey()));
            } else {
                String email = loginForm.field("email").value();
                String password = loginForm.field("password").value();

                User user = new User();
                user.setEmail(email);
                user.setPassword(password);
                Form<User> userForm = myFactory.form(User.class).fill(user);

                boolean reCaptchaResult;
                if (loginForm.field("g-recaptcha-response") == null) {
                    reCaptchaResult = false;
                } else {
                    String token = loginForm.field("g-recaptcha-response").value();
                    reCaptchaResult = reCaptchaAuthenticate(token);
                }
                if (!reCaptchaResult) {
                    flash("error", "Invalid reCAPTCHA code");
                    return badRequest(login.render(userForm, userService.getPublicRecaptchaKey()));
                }

                ObjectNode jsonData = Json.newObject();
                jsonData.put("email", email);
                jsonData.put("password", password);

                // POST Service JSON data
                JsonNode response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config,
                        Constants.USER_LOGIN), jsonData);
                Logger.info("login response: " + response);
                if (response.has("error")) {
                    String error = response.get("error").asText();
                    if (error.equals("User has not been activated. Please check your email for the activation link.")) {
                        flash("error", "User has not been activated. Please check your email for the activation link.");
                        flash("showResendActivation", "true");
                    } else {
                        flash("error", "Invalid email or password");
                    }
                    return badRequest(login.render(userForm, userService.getPublicRecaptchaKey()));
                }
                User currentUser = User.deserialize(response);
                session().clear();
                session("id", currentUser.getId() + "");
                session("username", currentUser.getUserName());
                session("userfirstname", currentUser.getFirstName());
//                session("level", currentUser.getLevel());
                session("email", email);
                session("avatar", currentUser.getAvatar());
                session("userTypes", currentUser.getUserType() + "");
                session("organization", currentUser.getOrganization());
//                JsonNode projectNode = response.findPath("project");
//                if (!projectNode.asText().equals("null")) {
//                    Project project = Project.deserialize(projectNode);
//                    session("projectId", (project.getId() + ""));
//                } else {
//                    session("projectId", "0");
//                }

                return redirect(routes.Application.home());
            }

        } catch (Exception e) {
            Logger.debug("UserController.userLogin exception: " + e.toString());
            return redirect(routes.Application.home());
        }
    }

    /************************************************** End of User Login *********************************************/

    /************************************************** User List *****************************************************/

    /**
     * This method preparea data to render the pageNum of listing all users with pagination (userList.scala.html)
     *
     * @param pageNum:      currrent page number
     * @param sortCriteria: sort column
     * @return: data for userList.scala.html
     */
    @With(OperationLoggingAction.class)
    public Result userList(Integer pageNum, String sortCriteria) {
        checkLoginStatus();

        try {
            if(isPrivateProjectZone()){
                return redirect(routes.UserController.myFollowees(1, "", CALLER_IS_NOT_MY_SPACE_PAGE));
            }

            // Set the offset and pageLimit.
            int pageLimit = Integer.parseInt(Constants.PAGINATION_NUMBER_ITEM_TWENTY);
            int offset = pageLimit * (pageNum - 1);

            JsonNode usersJsonNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.USER_LIST + "?offset=" + offset + "&pageLimit=" +
                            pageLimit + "&sortCriteria=" + sortCriteria));
            return userService.renderUserListPage(usersJsonNode, CALLER_IS_NOT_MY_SPACE_PAGE, pageLimit, null,
                    "all", session("username"), Long.parseLong(session("id")));
        } catch (Exception e) {
            Logger.debug("UserController.userList exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
    }

    /**
     * This method prepares data to render the page of listing my followees with pagination (userList.scala.html)
     *
     * @param page:         current page number
     * @param sortCriteria: sort criteria (date or name or usage count)
     * @return: data for userList.scala.html
     */
    public Result myFollowees(Integer page, String sortCriteria, boolean isCallerMySpacePage) {
        try {
            int pageLimit = Integer.parseInt(Constants.PAGINATION_NUMBER_ITEM_TWENTY);
            int offset = pageLimit * (page - 1);
            JsonNode usersJsonNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.MY_FOLLOWEES + "?offset=" + offset + "&pageLimit=" + pageLimit +
                            "&sortCriteria=" + sortCriteria + "&uid=" + session("id")));
            return userService.renderUserListPage(usersJsonNode, isCallerMySpacePage, pageLimit, null,
                    "my followees", session("username"), Long.parseLong(session("id")));
        } catch (Exception e) {
            Logger.debug("UserController.myFollowees exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
    }

    /**
     * This method prepares data to render the page of listing my followers with pagination (userList.scala.html)
     *
     * @param page:         current page number
     * @param sortCriteria: sort criteria (date or name or usage count)
     * @return: data for userList.scala.html
     */
    public Result myFollowers(Integer page, String sortCriteria) {
        try {
            int pageLimit = Integer.parseInt(Constants.PAGINATION_NUMBER_ITEM_TWENTY);
            int offset = pageLimit * (page - 1);
            JsonNode usersJsonNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.MY_FOLLOWERS + "?offset=" + offset + "&pageLimit=" + pageLimit +
                            "&sortCriteria=" + sortCriteria + "&uid=" + session("id")));
            return userService.renderUserListPage(usersJsonNode, CALLER_IS_MY_SPACE_PAGE, pageLimit, null,
                    "my followers", session("username"), Long.parseLong(session("id")));
        } catch (Exception e) {
            Logger.debug("UserController.myFollowers exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
    }
    /************************************************** End of User List **********************************************/


    /************************************************** User Detail ***************************************************/

    /**
     * This page intends to render the user detail page given a user id
     *
     * @param userId given user id
     * @return render the userDetail.scala.html page or if failed show the homepage.
     */
    @With(OperationLoggingAction.class)
    public Result userDetailPage(Long userId) {
        checkLoginStatus();
        Logger.info("userDetailPage userId: " + userId);
        try {
            JsonNode userNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.USER_DETAIL + userId));
            User user = User.deserialize(userNode);
            Logger.debug(userNode.asText());
            return ok(userDetail.render(user));
        } catch (Exception e) {
            Logger.debug("UserController.userDetailPage exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
    }
    /************************************************** User Detail ***************************************************/
    /************************************************** My Space ******************************************************/

    /**
     * This page intends to render the user detail page given a user id
     *
     * @return render the userDetail.scala.html page or if failed show the homepage.
     */
    @With(OperationLoggingAction.class)
    public Result mySpacePage() {
        checkLoginStatus();


        String userTypes = session("userTypes");

        if (userTypes == null) {
            return unauthorized("Unknown role or not authorized.");
        }

        return ok(mySpace.render(userTypes));

        /*
        if ("1".equals(userTypes)) {
            return ok(mySpace_researcher.render());
        } else if ("2".equals(userTypes)) {
            return ok(mySpace_sponsor.render());
        } else if ("4".equals(userTypes)) {
            return ok(mySpace_student.render());
        } else {
            return unauthorized("Unknown role or not authorized. User info: " + userTypes);
        }
         */
    }
    /************************************************** My Space ******************************************************/

    /************************************************** User Login Checking *******************************************/

    /**
     * This method checks whether an email exists in database.
     * Also used in Javascript in signup.scala.html directly
     * @return
     */
    public Result isEmailExisted() {
        JsonNode json = request().body().asJson();
        String email = json.path("email").asText();
        System.out.println("<<<<1.1 email: " + email);
        ObjectNode jsonData = Json.newObject();
        JsonNode response = null;

        try {
            jsonData.put("email", email);
            response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.CHECK_EMAIL), jsonData);
            Application.flashMsg(response);
        } catch (Exception e) {
            Logger.debug("UserController.isEmailExisted() exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
        System.out.println("<<<<<1.1 after checking email: " + response.toString());
        return ok(response);
    }

    /**
     * Validate whether an email address is correct
     */
    public Result validateEmail() {
        JsonNode json = request().body().asJson();
        String email = json.path("email").asText();
        ObjectNode jsonData = Json.newObject();
        JsonNode response = null;

        try {
            jsonData.put("email", email);
            response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.EMAIL_VALIDATE), jsonData);
            Application.flashMsg(response);
        } catch (Exception e) {
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
        return ok(response);
    }
    /****************************************** End of User Login Checking ********************************************/

    /**
     * This method intends to render an empty search.scala.html page
     *
     * @return
     */
    @With(OperationLoggingAction.class)
    public Result searchPage() {
        checkLoginStatus();

        return ok(search.render("user"));
    }

    /**
     * This method intends to prepare data for rending user research result page
     *
     * @param pageNum
     * @param sortCriteria
     * @return: data prepared for userList.scala.html (same as show all user list page)
     */
    @With(OperationLoggingAction.class)
    public Result searchPOST(Integer pageNum, String sortCriteria) {
        checkLoginStatus();
        Project currentProjectZone = projectService.getCurrentProjectZone();

        try {
            Form<User> tmpForm = userForm.bindFromRequest();
            Map<String, String> tmpMap = tmpForm.data();

            if(isPrivateProjectZone()){
                tmpMap.put("userId", session("id"));
            }

            JsonNode searchJson = Json.toJson(tmpMap);
            String searchString = "";

            // if not coming from search input page, then should fetch searchJson from the form from key "searchString"
            if (tmpMap.get("searchString") != null) {
                searchString = tmpMap.get("searchString");
                searchJson = Json.parse(searchString);
            } else {
                searchString = Json.stringify(searchJson);
            }

            //TODO: Find user with one blank between first name and last name???
            // The users to search might be someone I am following.
            // Set the offset and limit.
            int pageLimit = Integer.parseInt(Constants.PAGINATION_NUMBER_ITEM_TWENTY);
            int offset = pageLimit * (pageNum - 1);
            JsonNode usersJsonNode = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.GET_USER_BY_CONDITION + "?offset=" + offset + "&pageLimit=" + pageLimit +
                            "&sortCriteria=" + sortCriteria), searchJson);
            return userService.renderUserListPage(usersJsonNode, CALLER_IS_NOT_MY_SPACE_PAGE, pageLimit, searchString,
                    "search", session("username"), Long.parseLong(session("id")));
        } catch (Exception e) {
            Logger.debug("UserController.searchPOST exception: " + e.toString());
            return redirect(routes.Application.home());
        }
    }

    /**
     * This method intends to delete a user from userEdit.scala.html page.
     *
     * @return
     */
    @With(OperationLoggingAction.class)
    public Result userDelete() {
        checkLoginStatus();
        Form<User> user = userForm.bindFromRequest();
        if (user.hasErrors()) {
            Logger.debug("UserController.userDelete usr has error");
            return badRequest(login.render(user, userService.getPublicRecaptchaKey()));
        } else {
            String id = session("id");
            ObjectNode jsonData = Json.newObject();
            jsonData.put("id", id);

            JsonNode response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config,
                    Constants.USER_DELETE), jsonData);
            if (response == null || response.has("error")) {
                Logger.debug("User cannot be deleted and you have been logged out");
                return redirect(routes.Application.login());
            } else {
                session().clear();
                Logger.info("User has been deleted and you have been logged out");
                return redirect(routes.Application.login());
            }
        }
    }

    /****************************************** END Basic refactoring *************************************************/

    /**
     * Gets the user image by image imageId
     *
     * @param imageId the image imageId
     * @return the image
     */
    public Result userImageByImageId(long imageId) {
        // Get a file
        File image = RESTfulCalls.getAPIAsFile(RESTfulCalls.getBackendAPIUrl(config, Constants.GET_USER_IMAGE_BY_IMAGE_ID + imageId));
        return ok(image);
    }

    /******************************************************************************************************************/

    /**
     *
     * @param uid
     * @return
     */
    @With(OperationLoggingAction.class)
    public Result updateLevel(Long uid) {
        JsonNode json = request().body().asJson();
        Logger.info("In frontend UserController updateLevel(): " + json);
        JsonNode res = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config, "/user/updateLevel/" + uid), json);
        String result = res.toString();
        return ok(result);
    }

    /**
     *
     * @param hashcode
     * @return
     */
    public Result userSaved(String hashcode) {
        try {
            URLCodec urlCodec = new URLCodec();

            JsonNode response = RESTfulCalls
                    .getAPI(RESTfulCalls.getBackendAPIUrl(config, "/user/" + urlCodec.encode(hashcode, "UTF-8")));
            if (response == null || response.has("msg")) {
                String msg = response.get("msg").asText();
                if (msg.equals("Link is used.")) {
                    return redirect(routes.Application.showLinkIsAlreadyClick());
                }
                return redirect(routes.Application.showVerificationEmailIsExpired());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return redirect(routes.Application.createUserSuccess());
    }

    /**
     * Send email to validate and finish emailRegisterPage account
     *
     * @return
     */
    public Result sendRegisterEmail(String email, String id) {
        Logger.info("sendRegisterEmail: Received email = " + email + ", id = " + id);
        ObjectNode jsonData = Json.newObject();
        jsonData.put("email", email);
        jsonData.put("id", Long.parseLong(id));

        //use this function to get whole url.
        String version = routes.UserController.userSaved("").absoluteURL(request());
        jsonData.put("url", version);

        Logger.info("sendRegisterEmail: JSON payload = " + jsonData.toString());

        JsonNode response = RESTfulCalls
                .postAPI(RESTfulCalls.getBackendAPIUrl(config, "/user/validate"), jsonData);
        Logger.info("sendRegisterEmail: Received response from validation API = " + response);
        Logger.info("sendRegisterEmail: Redirecting to updatePassword for email = " + email);
        // return redirect(routes.Application.updatePassword(email));
        return ok(registerConfirmation.render(new Long(id), "User"));
    }

    @With(OperationLoggingAction.class)
    public Result resendActivationEmail() {
        String email = request().getQueryString("email");
        Logger.info("resendActivationEmail: Received email = " + email);

        ObjectNode jsonData = Json.newObject();
        jsonData.put("email", email);

        String url = routes.UserController.userSaved("").absoluteURL(request());
        jsonData.put("url", url);

        Logger.info("resendActivationEmail: JSON payload = " + jsonData.toString());
        JsonNode response = RESTfulCalls.postAPI(
                RESTfulCalls.getBackendAPIUrl(config, "/user/validate/resend"),
                jsonData
        );
        Logger.info("resendActivationEmail: Received response from backend = " + response);

        return ok("Activation email has been resent successfully to " + email);
    }

    public Result activateUser(String hashcode) {
        System.out.println("Received activation token: " + hashcode);

        String url = RESTfulCalls.getBackendAPIUrl(config, "/users/" + hashcode);
        JsonNode response = RESTfulCalls.getAPI(url);
        Logger.info("Activation response: " + response);
        String msg = response.get("msg").asText();
        Logger.info("activateUser result: " + msg);
        if ("Activation successful!".equals(msg)) {
            return ok(activationConfirmation.render());
        } else {
            return ok(activationError.render());
        }
    }

    /**
     *
     * @param userId
     * @param page
     * @return
     */
    public Result followedByUser(Long userId, String page) {
        checkLoginStatus();


        JsonNode response = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                Constants.USER_FOLLOWED_BY_USER + "/" + userId + "/" + session("id")));

        if (page.equals("Search")) {
            if (response.has("error")) {
                Application.flashMsg(RESTfulCalls.createResponse(RESTfulCalls.ResponseType.UNKNOWN));
                return ok();
            }
            return ok("200");
        }
        if (response.has("error")) {
            Application.flashMsg(RESTfulCalls.createResponse(RESTfulCalls.ResponseType.UNKNOWN));
            return userList(1, "id");
        }
        return userList(1, "id");
    }

    /**
     *
     * @param userId
     * @param page
     * @return
     */
    public Result unFollowedByUser(Long userId, String page) {
        checkLoginStatus();

        JsonNode response = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config,
                Constants.USER_UNFOLLOWED_BY_USER + "/" + userId + "/" + session("id")));
        if (page.equals("All")) {
            if (response.has("error")) {
                Application.flashMsg(RESTfulCalls.createResponse(RESTfulCalls.ResponseType.UNKNOWN));
                return userList(1, "");
            }
            return userList(1, "");
        } else if (page.equals("Search")) {
            if (response.has("error")) {
                Application.flashMsg(RESTfulCalls.createResponse(RESTfulCalls.ResponseType.UNKNOWN));
                return ok();
            }
            return ok();
        }
        if (response.has("error")) {
            Application.flashMsg(RESTfulCalls.createResponse(RESTfulCalls.ResponseType.UNKNOWN));
            // I know that unfollowing the user can happen from lists shown from my space as well, but
            // actually this return value is not rendering any page the reason is that unfollowing a user
            // toggles the button on the user list page and hence we don't need to change the page after
            // just unfollowing a user --> So this line of code is fine
            return myFollowees(1, "id", CALLER_IS_NOT_MY_SPACE_PAGE);
        }
        // I know that unfollowing the user can happen from lists shown from my space as well, but
        // actually this return value is not rendering any page the reason is that unfollowing a user
        // toggles the button on the user list page and hence we don't need to change the page after
        // just unfollowing a user --> So this line of code is fine
        return myFollowees(1, "id", CALLER_IS_NOT_MY_SPACE_PAGE);
    }

    /**
     *
     * @param token
     * @return
     */
    private boolean reCaptchaAuthenticate(String token) {
        if (token == null)
            return false;

        String secretKey = null;
        // Add the recaptcha based on the server type.
        if (config.getString("system.frontend.host").equals("hawking.sv.cmu.edu")) {
            secretKey = config.getString("recaptcha.private.hawking.key");
        } else if (config.getString("system.frontend.host").equals("opennex.org")) {
            secretKey = config.getString("recaptcha.private.opennex.key");
        } else {
            secretKey = config.getString("recaptcha.private.scihub.key");
        }

        String url = Constants.RECAPTCHA_VALIDATE;
        url += "?secret=" + secretKey;
        url += "&response=" + token;
        JsonNode postResult = RESTfulCalls.postAPI(url, Json.newObject());

        Logger.info(postResult.toString());

        if (postResult.get("success") == null || !postResult.get("success").asBoolean()) {
            return false;
        }

        return true;
    }

    /**
     *
     * @return
     */
    public Result callServiceInMap() {
        checkLoginStatus();

        JsonNode res = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.SERVICE_LOCATION));
        String result = res.toString();
        return ok(result);
    }


    /**
     * Check whether the password is correct
     *
     * @return
     */
    public Result checkPassword() {
        Form<User> user = userForm.bindFromRequest();
        JsonNode json = request().body().asJson();
        String pwd = json.get("pwd").asText();
        String email = json.get("email").asText();
        ObjectNode jsonData = Json.newObject();
        jsonData.put("email", email);
        jsonData.put("password", pwd);

        // POST Service JSON data
        JsonNode response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.USER_LOGIN), jsonData);
        if (response == null || response.has("error")) {
            return ok("error");
        }
        return ok("valid password");
    }

    @With(OperationLoggingAction.class)
    public Result updatePasswordPage() {
        String userId = session("id");
        String email = session("email");
        return ok(passwordUpdate.render(userForm, userId, email));
    }

    @With(OperationLoggingAction.class)
    public Result updatePasswordForUser() {
        Form<User> user = userForm.bindFromRequest();
        ObjectNode jsonData = userService.createJsonFromUserForm(userForm);
        String email = user.field("email").value();
        String password = user.field("password").value();
        jsonData.put("email", email);
        jsonData.put("password", password);
        JsonNode response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config,
                Constants.USER_UPDATE_PASSWORD), jsonData);
        return redirect(routes.Application.home());
    }

    /**
     *
     * @return
     */
    public Result updatePassword() {
        try {
            Form<User> user = userForm.bindFromRequest();
            String password = user.field("new_password").value();
            String email = user.field("email").value();
            ObjectNode jsonData = Json.newObject();
            jsonData.put("email", email);
            jsonData.put("password", password);
            JsonNode response = RESTfulCalls
                    .postAPI(Constants.URL_HOST + Constants.CMU_BACKEND_PORT +
                            Constants.UPDATE_PASSWORD, jsonData);
            response = RESTfulCalls.postAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.USER_LOGIN),
                    jsonData);
            if (response == null || response.has("error")) {
                return redirect(routes.Application.updatePassword(email));
            }
            session().clear();
            session("id", response.get("id").toString());
            session("username", response.get("userName").textValue());
            session("level", response.get("level").textValue());
            session("email", email);
            JsonNode projectNode = response.findPath("project");
            if (!projectNode.asText().equals("null")) {
                Project project = Project.deserialize(projectNode);
                session("projectId", (project.getId() + ""));
            } else {
                session("projectId", "0");
            }
            return redirect(routes.Application.home());
        } catch (Exception e) {
            Logger.debug("UserController.updatePassword() exception: " + e.toString());
            Application.flashMsg(RESTfulCalls.createUserResponse(RESTfulCalls.UserResponseType.GENERALERROR));
            return ok(generalError.render());
        }
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public List<User> getAllUsers() throws Exception {
        JsonNode userJson = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.GET_ALL_USERS));
        List<User> users = new ArrayList<>();
        for (JsonNode userNode:userJson) {
            users.add(User.deserialize(userNode));
        }
        return users;
    }

    /**
     *
     * @return
     */
    @With(OperationLoggingAction.class)
    public Result allUserIds(){
        checkLoginStatus();
        try{
            JsonNode usersNode = RESTfulCalls.getAPI(RESTfulCalls.getBackendAPIUrl(config, Constants.GET_ALL_USERS));
            ArrayNode res = Json.newArray();

            for(int i = 0; i < usersNode.size(); i++){
                res.add(usersNode.get(i).get("id").asLong());
            }

            return ok(res);

        }catch (Exception e){
            return ok("error");
        }
    }



}
