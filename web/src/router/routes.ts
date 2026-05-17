import type { RouteRecordRaw } from 'vue-router';
import DefaultLayout from '@/layouts/DefaultLayout.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/RegisterView.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/login/ForgotPasswordView.vue'),
    meta: { public: true, title: '忘记密码' }
  },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '数据总览', icon: 'odometer', menu: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', icon: 'avatar', menu: true }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/UsersList.vue'),
        meta: { title: '用户管理', icon: 'user-solid', menu: true, permission: 'SYSTEM_ADMIN' }
      },
      {
        path: 'my-clubs',
        name: 'MyClubs',
        component: () => import('@/views/clubs/MyClubs.vue'),
        meta: { title: '我的社团', icon: 'user-filled', menu: true, permission: 'NON_SYSTEM_ADMIN' }
      },
      {
        path: 'clubs',
        name: 'Clubs',
        component: () => import('@/views/clubs/ClubsList.vue'),
        meta: { title: '社团管理', icon: 'office-building', menu: true, permission: 'SYSTEM_ADMIN' }
      },
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/members/MembersList.vue'),
        // 仅加入过社团的用户可见（成员及以上）
        meta: { title: '成员管理', icon: 'user', menu: true, permission: 'CLUB_MEMBER' }
      },
      {
        path: 'activities',
        name: 'Activities',
        component: () => import('@/views/activities/ActivitiesList.vue'),
        meta: { title: '活动管理', icon: 'calendar', menu: true, permission: ['SYSTEM_ADMIN', 'CLUB_MEMBER'] }
      },
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('@/views/members/Attendance.vue'),
        // 干事及以上可见，普通成员不可见
        meta: { title: '考勤统计', icon: 'trend-charts', menu: true, permission: 'CLUB_STAFF' }
      },
      {
        path: 'member-applications',
        name: 'MemberApplications',
        component: () => import('@/views/members/Applications.vue'),
        // 仅社长 / 部长 / 副部长可见（有审批权）
        meta: { title: '入社申请审批', icon: 'tickets', menu: true, permission: 'CLUB_ADMIN' }
      },
      {
        path: 'club-applications',
        name: 'ClubApplications',
        component: () => import('@/views/clubs/Applications.vue'),
        meta: { title: '创建社团审批', icon: 'document-checked', menu: true, permission: 'SYSTEM_ADMIN' }
      },
      {
        path: 'club-explore',
        name: 'ClubExplore',
        component: () => import('@/views/clubs/ClubsExplore.vue'),
        meta: { title: '社团广场', icon: 'grid', menu: true }
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('@/views/messages/Announcements.vue'),
        meta: { title: '公告中心', icon: 'message-box', menu: true }
      },
      {
        path: 'messages',
        name: 'MessageCenter',
        component: () => import('@/views/messages/MessageCenter.vue'),
        meta: { title: '消息中心', icon: 'message', menu: true }
      },
      // hidden routes
      {
        path: 'activities/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/activities/ActivityDetail.vue'),
        meta: { title: '活动详情', hidden: true }
      },
      {
        path: 'club-create-apply',
        name: 'ClubCreateApply',
        component: () => import('@/views/clubs/ClubApply.vue'),
        meta: { title: '创建社团申请', hidden: true }
      },
      {
        path: 'clubs/:id',
        name: 'ClubDetail',
        component: () => import('@/views/clubs/ClubDetail.vue'),
        meta: { title: '社团详情', hidden: true }
      },
      {
        path: 'clubs/:id/join',
        name: 'ClubJoinApply',
        component: () => import('@/views/clubs/JoinClubApply.vue'),
        meta: { title: '申请加入社团', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/exception/NotFound.vue'),
    meta: { public: true, title: '404' }
  }
];

export default routes;

