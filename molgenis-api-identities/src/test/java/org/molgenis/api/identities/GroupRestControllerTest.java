package org.molgenis.api.identities;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.molgenis.api.identities.AddGroupMemberCommand.addGroupMember;
import static org.molgenis.api.identities.GroupCommand.createGroup;
import static org.molgenis.api.identities.GroupRestController.GROUP_END_POINT;
import static org.molgenis.api.identities.GroupRestController.TEMP_USER_END_POINT;
import static org.molgenis.api.identities.UpdateGroupMemberCommand.updateGroupMember;
import static org.molgenis.data.security.auth.GroupPermission.ADD_MEMBERSHIP;
import static org.molgenis.data.security.auth.GroupPermission.REMOVE_MEMBERSHIP;
import static org.molgenis.data.security.auth.GroupPermission.UPDATE_MEMBERSHIP;
import static org.molgenis.data.security.auth.GroupPermission.VIEW;
import static org.molgenis.data.security.auth.GroupPermission.VIEW_MEMBERSHIP;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.mockito.Mock;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.security.GroupIdentity;
import org.molgenis.data.security.auth.Group;
import org.molgenis.data.security.auth.GroupMetadata;
import org.molgenis.data.security.auth.GroupPermission;
import org.molgenis.data.security.auth.GroupPermissionService;
import org.molgenis.data.security.auth.GroupService;
import org.molgenis.data.security.auth.Role;
import org.molgenis.data.security.auth.RoleMembership;
import org.molgenis.data.security.auth.RoleMembershipMetadata;
import org.molgenis.data.security.auth.RoleMetadata;
import org.molgenis.data.security.auth.RoleService;
import org.molgenis.data.security.auth.User;
import org.molgenis.data.security.auth.UserMetadata;
import org.molgenis.data.security.exception.GroupNameNotAvailableException;
import org.molgenis.data.security.exception.GroupPermissionDeniedException;
import org.molgenis.data.security.exception.NotAValidGroupRoleException;
import org.molgenis.data.security.permission.RoleMembershipService;
import org.molgenis.data.security.user.UserService;
import org.molgenis.security.core.GroupValueFactory;
import org.molgenis.security.core.Permission;
import org.molgenis.security.core.UserPermissionEvaluator;
import org.molgenis.security.core.model.GroupValue;
import org.molgenis.test.AbstractMockitoTestNGSpringContextTests;
import org.molgenis.util.i18n.MessageSourceHolder;
import org.molgenis.util.i18n.TestAllPropertiesMessageSource;
import org.molgenis.util.i18n.format.MessageFormatFactory;
import org.molgenis.web.converter.GsonConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.NestedServletException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = {GroupRestControllerTest.Config.class, GsonConfig.class})
@TestExecutionListeners(listeners = WithSecurityContextTestExecutionListener.class)
public class GroupRestControllerTest extends AbstractMockitoTestNGSpringContextTests {
  private final GroupValueFactory groupValueFactory = new GroupValueFactory();
  @Mock private GroupService groupService;
  @Mock private GroupPermissionService groupPermissionService;
  @Mock private RoleMembershipService roleMembershipService;
  @Mock private RoleService roleService;
  @Mock private UserService userService;

  @Mock private UserPermissionEvaluator userPermissionEvaluator;

  @Mock private RoleMembershipMetadata roleMembershipMetadata;
  @Mock private RoleMetadata roleMetadata;
  @Mock private GroupMetadata groupMetadata;
  @Mock private UserMetadata userMetadata;
  @Mock private Attribute attribute;

  @Mock private User user;
  @Mock private Group group;
  @Mock private Role viewer;
  @Mock private Role editor;
  @Mock private Role manager;
  @Mock private Role anonymous;
  @Mock private LocaleResolver localeResolver;
  @Mock private RoleMembership memberShip;

  private MockMvc mockMvc;

  @Autowired private GsonHttpMessageConverter gsonHttpMessageConverter;

  @Autowired private Gson gson;

  @BeforeClass
  public void beforeClass() {
    TestAllPropertiesMessageSource messageSource =
        new TestAllPropertiesMessageSource(new MessageFormatFactory());
    messageSource.addMolgenisNamespaces("data-security", "data", "security");
    MessageSourceHolder.setMessageSource(messageSource);
  }

  @BeforeMethod
  public void beforeMethod() {
    GroupRestController groupRestController =
        new GroupRestController(
            groupValueFactory,
            groupService,
            roleMembershipService,
            roleService,
            userService,
            userPermissionEvaluator,
            groupPermissionService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(groupRestController)
            .setMessageConverters(new FormHttpMessageConverter(), gsonHttpMessageConverter)
            .setLocaleResolver(localeResolver)
            .build();
  }

  @Test
  @WithMockUser("henkie")
  public void testCreateGroup() throws Exception {
    when(groupService.isGroupNameAvailable(any())).thenReturn(true);
    mockMvc
        .perform(
            post(GROUP_END_POINT)
                .contentType(APPLICATION_JSON_UTF8)
                .content(gson.toJson(createGroup("devs", "Developers"))))
        .andExpect(status().isCreated());

    GroupValue groupValue =
        groupValueFactory.createGroup(
            "devs", "Developers", null, true, ImmutableSet.of("Manager", "Editor", "Viewer"));
    verify(groupService).persist(groupValue);
    verify(groupPermissionService).grantDefaultPermissions(groupValue);
    verify(roleMembershipService).addUserToRole("henkie", "DEVS_MANAGER");
  }

  @Test(
      expectedExceptions = GroupNameNotAvailableException.class,
      expectedExceptionsMessageRegExp = "groupName:devs")
  @WithMockUser("henkie")
  public void testCreateGroupUnavailableGroupName() throws Throwable {
    when(groupService.isGroupNameAvailable(any())).thenReturn(false);
    try {
      mockMvc
          .perform(
              post(GROUP_END_POINT)
                  .contentType(APPLICATION_JSON_UTF8)
                  .content(gson.toJson(createGroup("devs", "Developers"))))
          .andExpect(status().isBadRequest());
    } catch (NestedServletException e) {
      verifyNoMoreInteractions(groupService);
      verifyNoMoreInteractions(groupPermissionService);
      verifyNoMoreInteractions(roleMembershipService);
      throw e.getCause();
    }
  }

  @Test
  public void testGetGroups() throws Exception {
    when(groupService.getGroups()).thenReturn(singletonList(group));
    when(group.getName()).thenReturn("devs");
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW)).thenReturn(true);
    when(group.getLabel()).thenReturn("Developers");

    mockMvc
        .perform(get(GROUP_END_POINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("devs")))
        .andExpect(jsonPath("$[0].label", is("Developers")));
  }

  @Test
  public void testGetGroupPermissionDenied() throws Exception {
    when(groupService.getGroups()).thenReturn(singletonList(group));
    when(group.getName()).thenReturn("devs");
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW)).thenReturn(false);

    mockMvc
        .perform(get(GROUP_END_POINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  public void testGetMembers() throws Exception {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    when(group.getRoles()).thenReturn(ImmutableList.of(viewer, editor, manager));

    when(roleMembershipService.getMemberships(ImmutableList.of(viewer, editor, manager)))
        .thenReturn(singletonList(memberShip));

    when(memberShip.getUser()).thenReturn(user);
    when(memberShip.getRole()).thenReturn(editor);

    when(user.getUsername()).thenReturn("henkie");
    when(user.getId()).thenReturn("userId");

    when(editor.getName()).thenReturn("DEVS_EDITOR");
    when(editor.getLabel()).thenReturn("Developers Editor");

    mockMvc
        .perform(get(GROUP_END_POINT + "/devs/member"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].user.username", is("henkie")))
        .andExpect(jsonPath("$[0].user.id", is("userId")))
        .andExpect(jsonPath("$[0].role.roleName", is("DEVS_EDITOR")))
        .andExpect(jsonPath("$[0].role.roleLabel", is("Developers Editor")));
  }

  @Test(
      expectedExceptions = GroupPermissionDeniedException.class,
      expectedExceptionsMessageRegExp = "permission:VIEW_MEMBERSHIP groupName:devs")
  public void testGetMembersPermissionDenied() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW_MEMBERSHIP))
        .thenReturn(false);
    try {
      mockMvc.perform(get(GROUP_END_POINT + "/devs/member"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Group id:devs attribute:Name")
  public void testGetMembersUnknownGroup() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW_MEMBERSHIP))
        .thenReturn(true);
    when(groupMetadata.getId()).thenReturn("Group");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(groupMetadata, attribute, "devs"))
        .when(groupService)
        .getGroup("devs");
    try {
      mockMvc.perform(get(GROUP_END_POINT + "/devs/member"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testAddMembership() throws Exception {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), ADD_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    when(roleService.getRole("DEVS_EDITOR")).thenReturn(editor);
    when(userService.getUser("henkie")).thenReturn(user);

    mockMvc
        .perform(
            post(GROUP_END_POINT + "/devs/member")
                .contentType(APPLICATION_JSON_UTF8)
                .content(gson.toJson(addGroupMember("henkie", "DEVS_EDITOR"))))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    "http://localhost" + GROUP_END_POINT + "/devs/member/devs/member/henkie"));

    verify(groupService).addMember(group, user, editor);
  }

  @Test(
      expectedExceptions = GroupPermissionDeniedException.class,
      expectedExceptionsMessageRegExp = "permission:ADD_MEMBERSHIP groupName:devs")
  public void testAddMembershipPermissionDenied() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), ADD_MEMBERSHIP))
        .thenReturn(false);
    try {
      mockMvc.perform(
          post(GROUP_END_POINT + "/devs/member")
              .contentType(APPLICATION_JSON_UTF8)
              .content(gson.toJson(addGroupMember("user", "DEVS_EDITOR"))));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Group id:devs attribute:Name")
  public void testAddMembershipUnknownGroup() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), ADD_MEMBERSHIP))
        .thenReturn(true);

    when(groupMetadata.getId()).thenReturn("Group");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(groupMetadata, attribute, "devs"))
        .when(groupService)
        .getGroup("devs");
    try {
      mockMvc.perform(
          post(GROUP_END_POINT + "/devs/member")
              .contentType(APPLICATION_JSON_UTF8)
              .content(gson.toJson(addGroupMember("user", "DEVS_EDITOR"))));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Role id:DEVS_EDITOR attribute:Name")
  public void testAddMembershipUnknownRole() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), ADD_MEMBERSHIP))
        .thenReturn(true);

    when(groupService.getGroup("devs")).thenReturn(group);
    when(roleMetadata.getId()).thenReturn("Role");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(roleMetadata, attribute, "DEVS_EDITOR"))
        .when(roleService)
        .getRole("DEVS_EDITOR");

    try {
      mockMvc.perform(
          post(GROUP_END_POINT + "/devs/member")
              .contentType(APPLICATION_JSON_UTF8)
              .content(gson.toJson(addGroupMember("user", "DEVS_EDITOR"))));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:User id:henkie attribute:Name")
  public void testAddMembershipUnknownUser() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), ADD_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    when(roleService.getRole("DEVS_EDITOR")).thenReturn(editor);
    when(userMetadata.getId()).thenReturn("User");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(userMetadata, attribute, "henkie"))
        .when(userService)
        .getUser("user");
    try {
      mockMvc.perform(
          post(GROUP_END_POINT + "/devs/member")
              .contentType(APPLICATION_JSON_UTF8)
              .content(gson.toJson(addGroupMember("user", "DEVS_EDITOR"))));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testRemoveMembership() throws Exception {
    when(groupService.getGroup("devs")).thenReturn(group);
    when(userService.getUser("henkie")).thenReturn(user);
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), REMOVE_MEMBERSHIP))
        .thenReturn(true);
    mockMvc
        .perform(delete("/api/identities/group/devs/member/henkie"))
        .andExpect(status().isNoContent());
    verify(groupService).removeMember(group, user);
  }

  @Test(
      expectedExceptions = GroupPermissionDeniedException.class,
      expectedExceptionsMessageRegExp = "permission:REMOVE_MEMBERSHIP groupName:devs")
  public void testRemoveMembershipPermissionDenied() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), REMOVE_MEMBERSHIP))
        .thenReturn(false);
    try {
      mockMvc.perform(delete("/api/identities/group/devs/member/henkie"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Group id:devs attribute:Name")
  public void testRemoveMembershipUnknownGroup() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), REMOVE_MEMBERSHIP))
        .thenReturn(true);
    when(groupMetadata.getId()).thenReturn("Group");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(groupMetadata, attribute, "devs"))
        .when(groupService)
        .getGroup("devs");
    try {
      mockMvc.perform(delete("/api/identities/group/devs/member/henkie"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:sys_sec_User id:henkie attribute:null")
  public void testRemoveMembershipUnknownUser() throws Throwable {
    when(groupService.getGroup("devs")).thenReturn(group);
    when(userService.getUser("henkie"))
        .thenThrow(new UnknownEntityException(UserMetadata.USER, "henkie"));
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), REMOVE_MEMBERSHIP))
        .thenReturn(true);
    try {
      mockMvc.perform(delete("/api/identities/group/devs/member/henkie"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Role Membership id:henkie attribute:User")
  public void testRemoveMembershipNotAMember() throws Throwable {
    when(groupService.getGroup("devs")).thenReturn(group);
    when(userService.getUser("henkie")).thenReturn(user);
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), REMOVE_MEMBERSHIP))
        .thenReturn(true);

    when(roleMembershipMetadata.getId()).thenReturn("Role Membership");
    when(attribute.getName()).thenReturn("User");
    doThrow(new UnknownEntityException(roleMembershipMetadata, attribute, "henkie"))
        .when(groupService)
        .removeMember(group, user);
    try {
      mockMvc.perform(delete("/api/identities/group/devs/member/henkie"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testUpdateMembership() throws Exception {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    when(userService.getUser("henkie")).thenReturn(user);
    when(roleService.getRole("DEVS_EDITOR")).thenReturn(editor);

    mockMvc
        .perform(
            put(GROUP_END_POINT + "/devs/member/henkie")
                .content(gson.toJson(updateGroupMember("DEVS_EDITOR")))
                .contentType(APPLICATION_JSON_UTF8))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Location"));

    verify(groupService).updateMemberRole(group, user, editor);
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Group id:devs attribute:Name")
  public void testUpdateMembershipUnknownGroup() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(true);
    when(groupMetadata.getId()).thenReturn("Group");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(groupMetadata, attribute, "devs"))
        .when(groupService)
        .getGroup("devs");

    try {
      mockMvc.perform(
          put(GROUP_END_POINT + "/devs/member/henkie")
              .content(gson.toJson(updateGroupMember("DEVS_EDITOR")))
              .contentType(APPLICATION_JSON_UTF8));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:User id:henkie attribute:Name")
  public void testUpdateMembershipUnknownUser() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);

    when(userMetadata.getId()).thenReturn("User");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(userMetadata, attribute, "henkie"))
        .when(userService)
        .getUser("henkie");

    try {
      mockMvc.perform(
          put(GROUP_END_POINT + "/devs/member/henkie")
              .content(gson.toJson(updateGroupMember("DEVS_EDITOR")))
              .contentType(APPLICATION_JSON_UTF8));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = UnknownEntityException.class,
      expectedExceptionsMessageRegExp = "type:Role id:DEVS_EDITOR attribute:Name")
  public void testUpdateMembershipUnknownRole() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    when(userService.getUser("henkie")).thenReturn(user);
    when(roleMetadata.getId()).thenReturn("Role");
    when(attribute.getName()).thenReturn("Name");
    doThrow(new UnknownEntityException(roleMetadata, attribute, "DEVS_EDITOR"))
        .when(roleService)
        .getRole("DEVS_EDITOR");

    try {
      mockMvc.perform(
          put(GROUP_END_POINT + "/devs/member/henkie")
              .content(gson.toJson(updateGroupMember("DEVS_EDITOR")))
              .contentType(APPLICATION_JSON_UTF8));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = GroupPermissionDeniedException.class,
      expectedExceptionsMessageRegExp = "permission:UPDATE_MEMBERSHIP groupName:devs")
  public void testUpdateMembershipPermissionDenied() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(false);

    try {
      mockMvc.perform(
          put(GROUP_END_POINT + "/devs/member/henkie")
              .contentType(APPLICATION_JSON_UTF8)
              .content(gson.toJson(updateGroupMember("DEVS_MANAGER"))));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test(
      expectedExceptions = NotAValidGroupRoleException.class,
      expectedExceptionsMessageRegExp = "role:DEVS_EDITOR group:devs")
  public void testUpdateMembershipNotAValidGroupRole() throws Throwable {
    when(groupService.getGroup("devs")).thenReturn(group);
    when(userService.getUser("henkie")).thenReturn(user);
    when(roleService.getRole("DEVS_EDITOR")).thenReturn(editor);
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(true);

    doThrow(new NotAValidGroupRoleException(editor, group))
        .when(groupService)
        .updateMemberRole(group, user, editor);

    when(group.getName()).thenReturn("devs");
    when(editor.getName()).thenReturn("DEVS_EDITOR");
    try {
      mockMvc.perform(
          put(GROUP_END_POINT + "/devs/member/henkie")
              .content(gson.toJson(updateGroupMember("DEVS_EDITOR")))
              .contentType(APPLICATION_JSON_UTF8));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testUpdateExtendsRole() throws Exception {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), UPDATE_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    doReturn(editor).when(roleService).getRole("DEVS_EDITOR");
    doReturn(anonymous).when(roleService).getRole("anonymous");

    mockMvc
        .perform(
            put(GROUP_END_POINT + "/devs/role/anonymous")
                .contentType(APPLICATION_JSON_UTF8)
                .content(gson.toJson(UpdateIncludeCommand.create("DEVS_EDITOR"))))
        .andExpect(status().isNoContent());

    verify(groupService).updateExtendsRole(group, editor, anonymous);
  }

  @Test
  public void testRemoveExtendsRole() throws Exception {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), REMOVE_MEMBERSHIP))
        .thenReturn(true);
    when(groupService.getGroup("devs")).thenReturn(group);
    doReturn(anonymous).when(roleService).getRole("anonymous");

    mockMvc
        .perform(
            delete(GROUP_END_POINT + "/devs/role/anonymous").contentType(APPLICATION_JSON_UTF8))
        .andExpect(status().isNoContent());

    verify(groupService).removeExtendsRole(group, anonymous);
  }

  @Test
  public void testGetGroupRoles() throws Exception {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW)).thenReturn(true);

    when(editor.getLabel()).thenReturn("role-label");
    when(editor.getName()).thenReturn("role-name");
    Iterable<Role> groupRoles = singletonList(editor);
    when(group.getRoles()).thenReturn(groupRoles);
    when(groupService.getGroup("devs")).thenReturn(group);

    mockMvc
        .perform(get(GROUP_END_POINT + "/devs/role/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].roleName", is("role-name")))
        .andExpect(jsonPath("$[0].roleLabel", is("role-label")));
  }

  @Test(
      expectedExceptions = GroupPermissionDeniedException.class,
      expectedExceptionsMessageRegExp = "permission:VIEW groupName:devs")
  public void testGetGroupRolesPermissionDenied() throws Throwable {
    when(userPermissionEvaluator.hasPermission(new GroupIdentity("devs"), VIEW)).thenReturn(false);
    try {
      mockMvc.perform(get(GROUP_END_POINT + "/devs/role/"));
    } catch (NestedServletException e) {
      throw e.getCause();
    }
  }

  @Test
  @WithMockUser(roles = {"MANAGER"})
  public void testGetUsers() throws Exception {
    when(user.getId()).thenReturn("id");
    when(user.getUsername()).thenReturn("name");

    User anonymousUser = mock(User.class);
    when(anonymousUser.getUsername()).thenReturn("anonymous");

    when(userService.getUsers()).thenReturn(Arrays.asList(user, anonymousUser));

    mockMvc
        .perform(get(TEMP_USER_END_POINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is("id")))
        .andExpect(jsonPath("$[0].username", is("name")));
  }

  @Test
  public void testPermissions() throws Exception {
    Set<Permission> set = new HashSet<>(Collections.singletonList(GroupPermission.ADD_MEMBERSHIP));

    when(userPermissionEvaluator.getPermissions(
            new GroupIdentity("devs"), GroupPermission.values()))
        .thenReturn(set);

    mockMvc
        .perform(get(GROUP_END_POINT + "/devs/permission"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]", is("ADD_MEMBERSHIP")));
  }

  @Configuration
  public static class Config {}

  @Test
  public void testDeleteGroup() throws Exception {
    mockMvc
        .perform(delete(GROUP_END_POINT + "/devs").contentType(APPLICATION_JSON_UTF8))
        .andExpect(status().isNoContent());
    verify(groupService).deleteGroup("devs");
  }
}
