


export const useProjectStore = defineStore('project', {
    state: () => ({
        project: {}
    }),
    actions: {
        // 设置整个空间对象
        setProject(newProject) {
            this.project = newProject
        },
        // 设置空间中的某个属性
        setProjectField(field, value) {
            this.project[field] = value
        }
    }
})

