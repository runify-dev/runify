import {Permission, Role, Group, Operate} from '@/permission/common'

export const PermissionConstants = {
  APPLICATION_READ: new Permission(Group.APPLICATION, Group.APPLICATION, Operate.READ, 0),
  APPLICATION_EDIT: new Permission(Group.APPLICATION, Group.APPLICATION, Operate.EDIT, 1),
  APPLICATION_DELETE: new Permission(Group.APPLICATION, Group.APPLICATION, Operate.DELETE, 2),
  APPLICATION_CREATE: new Permission(Group.APPLICATION, Group.APPLICATION, Operate.CREATE, 3),
  APPLICATION_FOLDER_EDIT: new Permission(Group.APPLICATION, Group.FOLDER, Operate.EDIT, 4),
  APPLICATION_FOLDER_DELETE: new Permission(Group.APPLICATION, Group.FOLDER, Operate.DELETE, 5),
  APPLICATION_FOLDER_CREATE: new Permission(Group.APPLICATION, Group.FOLDER, Operate.CREATE, 6),
  APPLICATION_OVERVIEW_READ: new Permission(Group.APPLICATION, Group.OVERVIEW, Operate.READ, 7),
  APPLICATION_SETTING_READ: new Permission(Group.APPLICATION, Group.SETTING, Operate.READ, 8),
  APPLICATION_CONVERSATION_LOG_READ: new Permission(
    Group.APPLICATION,
    Group.CONVERSATION_LOG,
    Operate.READ,
    9
  ),

  NOTE_READ: new Permission(Group.NOTE, Group.NOTE, Operate.READ, 0),
  NOTE_EDIT: new Permission(Group.NOTE, Group.NOTE, Operate.EDIT, 1),
  NOTE_DELETE: new Permission(Group.NOTE, Group.NOTE, Operate.DELETE, 2),
  NOTE_CREATE: new Permission(Group.NOTE, Group.NOTE, Operate.CREATE, 3),
  NOTE_FOLDER_EDIT: new Permission(Group.NOTE, Group.FOLDER, Operate.EDIT, 4),
  NOTE_FOLDER_DELETE: new Permission(Group.NOTE, Group.FOLDER, Operate.DELETE, 5),
  NOTE_FOLDER_CREATE: new Permission(Group.NOTE, Group.FOLDER, Operate.CREATE, 6),

  MODEL_READ: new Permission(Group.MODEL, Group.MODEL, Operate.READ, 0),
  MODEL_EDIT: new Permission(Group.MODEL, Group.MODEL, Operate.EDIT, 1),
  MODEL_DELETE: new Permission(Group.MODEL, Group.MODEL, Operate.DELETE, 2),
  MODEL_CREATE: new Permission(Group.MODEL, Group.MODEL, Operate.CREATE, 3),
  MODEL_FOLDER_EDIT: new Permission(Group.MODEL, Group.FOLDER, Operate.EDIT, 4),
  MODEL_FOLDER_DELETE: new Permission(Group.MODEL, Group.FOLDER, Operate.DELETE, 5),
  MODEL_FOLDER_CREATE: new Permission(Group.MODEL, Group.FOLDER, Operate.CREATE, 6),

  PROJECT_READ: new Permission(Group.PROJECT, Group.PROJECT, Operate.READ, 0),
  PROJECT_EDIT: new Permission(Group.PROJECT, Group.PROJECT, Operate.EDIT, 1),
  PROJECT_DELETE: new Permission(Group.PROJECT, Group.PROJECT, Operate.DELETE, 2),
  PROJECT_CREATE: new Permission(Group.PROJECT, Group.PROJECT, Operate.CREATE, 3),
  PROJECT_FOLDER_EDIT: new Permission(Group.PROJECT, Group.FOLDER, Operate.EDIT, 4),
  PROJECT_FOLDER_DELETE: new Permission(Group.PROJECT, Group.FOLDER, Operate.DELETE, 5),
  PROJECT_FOLDER_CREATE: new Permission(Group.PROJECT, Group.FOLDER, Operate.CREATE, 6),

  USER_MANAGEMENT_READ: new Permission(
    Group.SYSTEM_SETTING,
    Group.USER_MANAGEMENT,
    Operate.READ,
    0
  ),
  USER_MANAGEMENT_EDIT: new Permission(
    Group.SYSTEM_SETTING,
    Group.USER_MANAGEMENT,
    Operate.EDIT,
    1
  ),
  USER_MANAGEMENT_DELETE: new Permission(
    Group.SYSTEM_SETTING,
    Group.USER_MANAGEMENT,
    Operate.DELETE,
    2
  ),
  USER_MANAGEMENT_CREATE: new Permission(
    Group.SYSTEM_SETTING,
    Group.USER_MANAGEMENT,
    Operate.CREATE,
    3
  ),
  USER_MANAGEMENT_AUTHORIZATION: new Permission(
    Group.SYSTEM_SETTING,
    Group.USER_MANAGEMENT,
    Operate.AUTHORIZATION,
    4
  ),

  ROLE_MANAGEMENT_READ: new Permission(
    Group.ROLE_MANAGEMENT,
    Group.ROLE_MANAGEMENT,
    Operate.READ,
    0
  ),
  ROLE_MANAGEMENT_EDIT: new Permission(
    Group.USER_MANAGEMENT,
    Group.ROLE_MANAGEMENT,
    Operate.EDIT,
    1
  ),
  ROLE_MANAGEMENT_DELETE: new Permission(
    Group.USER_MANAGEMENT,
    Group.ROLE_MANAGEMENT,
    Operate.DELETE,
    2
  ),
  ROLE_MANAGEMENT_CREATE: new Permission(
    Group.USER_MANAGEMENT,
    Group.ROLE_MANAGEMENT,
    Operate.CREATE,
    3
  ),
  DATASOURCE_READ: new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.READ, 0),
  DATASOURCE_EDIT: new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.EDIT, 1),
  DATASOURCE_DELETE: new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.DELETE, 2),
  DATASOURCE_CREATE: new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.CREATE, 3),
  DATASOURCE_FOLDER_EDIT: new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.EDIT, 4),
  DATASOURCE_FOLDER_DELETE: new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.DELETE, 5),

}
