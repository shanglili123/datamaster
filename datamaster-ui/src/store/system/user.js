
import { login, logout, getInfo } from '@/api/system/login.js';
import { getToken, setToken, removeToken } from '@/utils/auth';
import defAva from '@/assets/images/defaultAvatar.svg';
import { currentUser } from '@/api/tax/space/space';
import { selectAccessibleSpace } from '@/utils/spaceSelection';

let spacesRequest = null;
let spacesVersion = 0;

const useUserStore = defineStore('user', {
    state: () => ({
        token: getToken(),
        id: '',
        name: '',
        nickName: '',
        phonenumber: '',
        avatar: '',
        roles: [],
        permissions: [],
        spaceId: '',
        spaceCode: '',
        spaceName: '',
        spaces: [],
        spacesLoading: false
    }),
    actions: {
        // 登录
        login(userInfo) {
            const username = userInfo.username.trim();
            const password = userInfo.password;
            const code = userInfo.code;
            const uuid = userInfo.uuid;
            return new Promise((resolve, reject) => {
                login(username, password, code, uuid)
                    .then((res) => {
                        setToken(res.token);
                        this.token = res.token;
                        resolve();
                    })
                    .catch((error) => {
                        reject(error);
                    });
            });
        },
        // 获取用户信息
        getInfo() {
            return new Promise((resolve, reject) => {
                getInfo()
                    .then(async (res) => {
                        const user = res.user;
                        const avatar =
                            user.avatar == '' || user.avatar == null
                                ? defAva
                                : import.meta.env.VITE_APP_BASE_API + user.avatar;

                        if (res.roles && res.roles.length > 0) {
                            this.roles = res.roles;
                            this.permissions = res.permissions;
                        } else {
                            this.roles = ['ROLE_DEFAULT'];
                        }
                        this.id = user.userId;
                        this.name = user.userName;
                        this.avatar = avatar;
                        this.nickName = user.nickName;
                        this.phonenumber = user.phonenumber;

                        await this.initializeSpace();
                        resolve(res);
                    })
                    .catch((error) => {
                        reject(error);
                    });
            });
        },
        setCurrentSpace(space) {
            const selected = space?.id != null
                ? this.spaces.find(item => String(item.id) === String(space.id))
                : null;
            if (space && !selected) throw new Error('请选择当前用户可访问的空间');
            this.$patch({
                spaceId: selected?.id || '',
                spaceCode: selected?.code || selected?.spaceCode || '',
                spaceName: selected?.name || selected?.spaceName || ''
            });
            if (selected) localStorage.setItem('dataMasterSpaceId', String(selected.id));
            else localStorage.removeItem('dataMasterSpaceId');
            return selected;
        },
        async loadSpaces() {
            if (spacesRequest) return spacesRequest;
            const version = ++spacesVersion;
            this.spacesLoading = true;
            spacesRequest = (async () => {
                try {
                    const response = await currentUser();
                    if (version !== spacesVersion) return [];
                    if (!Array.isArray(response?.data)) throw new Error('空间列表响应无效');
                    this.spaces = response.data.filter(space => selectAccessibleSpace([space], space?.id));
                    const preferred = this.spaceId || localStorage.getItem('dataMasterSpaceId');
                    this.setCurrentSpace(selectAccessibleSpace(this.spaces, preferred));
                    return this.spaces;
                } finally {
                    if (version === spacesVersion) {
                        this.spacesLoading = false;
                        spacesRequest = null;
                    }
                }
            })();
            return spacesRequest;
        },
        async initializeSpace() {
            try {
                await this.loadSpaces();
                return selectAccessibleSpace(this.spaces, this.spaceId);
            } catch (error) {
                this.setCurrentSpace(null);
                throw error;
            }
        },
        // 退出系统
        logOut() {
            return new Promise((resolve, reject) => {
                logout(this.token)
                    .then(() => {
                        this.token = '';
                        this.roles = [];
                        this.permissions = [];
                        ++spacesVersion;
                        spacesRequest = null;
                        this.spaces = [];
                        this.spacesLoading = false;
                        this.spaceId = '';
                        this.spaceCode = '';
                        this.spaceName = '';
                        localStorage.removeItem('dataMasterSpaceId');
                        removeToken();
                        resolve();
                    })
                    .catch((error) => {
                        reject(error);
                    });
            });
        },
        // 设置 Token
        setToken(token) {
            return new Promise((resolve, reject) => {
                setToken(token);
                this.token = token;
                resolve();
            });
        }
    }
});

export default useUserStore;

