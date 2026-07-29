


export const useSpaceStore = defineStore('space', {
    state: () => ({
        space: {}
    }),
    actions: {
        // 设置整个空间对象
        setSpace(newSpace) {
            this.space = newSpace
        },
        // 设置空间中的某个属性
        setSpaceField(field, value) {
            this.space[field] = value
        }
    }
})

