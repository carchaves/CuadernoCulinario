const ACCESS_KEY = "cocina:accessToken";
const REFRESH_KEY = "cocina:refreshToken";

type Listener = () => void;
const listeners = new Set<Listener>();
const notify = () => listeners.forEach((fn) => fn());

export const tokenStore = {
  getAccess: () => window.localStorage.getItem(ACCESS_KEY),
  getRefresh: () => window.localStorage.getItem(REFRESH_KEY),
  set(access: string, refresh: string) {
    window.localStorage.setItem(ACCESS_KEY, access);
    window.localStorage.setItem(REFRESH_KEY, refresh);
    notify();
  },
  setAccess(access: string) {
    window.localStorage.setItem(ACCESS_KEY, access);
  },
  clear() {
    window.localStorage.removeItem(ACCESS_KEY);
    window.localStorage.removeItem(REFRESH_KEY);
    notify();
  },
  hasSession: () => !!window.localStorage.getItem(REFRESH_KEY),
  subscribe(fn: Listener) {
    listeners.add(fn);
    return () => {
      listeners.delete(fn);
    };
  },
};
