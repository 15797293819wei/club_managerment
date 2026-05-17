import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { getToken, clearToken } from '@/utils/auth';

interface ApiResponse<T> {
  code: number;
  message: string;
  data?: T;
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const res: ApiResponse<unknown> = response.data;
    if (res && typeof res.code === 'number') {
      if (res.code === 0) {
        return res.data as unknown;
      }
      if (res.code === 401) {
        clearToken();
        if (router.currentRoute.value.name !== 'Login') {
          router.replace({ name: 'Login' });
        }
      }
      const tips = res.code >= 500
        ? '如多次出现，请联系管理员或稍后重试'
        : '请检查填写信息或稍后重试';
      ElMessage.error((res.message || '请求失败') + `（${tips}）`);
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return response.data;
  },
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;
    if (status === 401) {
      clearToken();
      if (router.currentRoute.value.name !== 'Login') {
        ElMessage.error('登录已过期，请重新登录');
        router.replace({ name: 'Login' });
      }
    } else {
      const tips = status && status >= 500
        ? '服务器开小差了，请稍后再试或联系管理员'
        : '请求失败，请检查网络或稍后重试';
      ElMessage.error((message || '请求失败') + `（${tips}）`);
    }
    return Promise.reject(error);
  }
);

export default http;
