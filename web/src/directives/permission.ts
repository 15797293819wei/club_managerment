import type { App, DirectiveBinding } from 'vue';
import { useAuthStore } from '@/store/modules/auth';

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const auth = useAuthStore();
  const required = binding.value as string | string[] | undefined;
  const has = auth.hasPermission(required);
  el.style.display = has ? '' : 'none';
}

export default {
  install(app: App) {
    app.directive('permission', {
      mounted(el, binding) {
        checkPermission(el, binding);
      },
      updated(el, binding) {
        checkPermission(el, binding);
      }
    });
  }
};

