
import { login, logout, getInfo } from '@/api/system/login.js';
import { getToken, setToken, removeToken } from '@/utils/auth';
import defAva from '@/assets/images/defaultAvatar.svg';
import { currentUser } from '@/api/tax/space/space';
import { selectAccessibleSpace } from '@/utils/spaceSelection';

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
        spaceName: ''
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
        async initializeSpace() {
            const preferred = this.spaceId || localStorage.getItem('dataMasterSpaceId');
            try {
                const response = await currentUser();
                const space = selectAccessibleSpace(response?.data, preferred);
                this.spaceId = space?.id || '';
                this.spaceCode = space?.code || space?.spaceCode || '';
                this.spaceName = space?.name || space?.spaceName || '';
                if (space) localStorage.setItem('dataMasterSpaceId', String(space.id));
                else localStorage.removeItem('dataMasterSpaceId');
                return space;
            } catch (error) {
                this.spaceId = '';
                this.spaceCode = '';
                this.spaceName = '';
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

