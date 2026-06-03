
import integratio from './integratio/integratio'
import developTask from './task/developTask'
import integratioTask from './task/integratioTask'
import asset from './asset/asset'

// 合并所有 col 模块下的路由
const colRouter = [
    ...integratio,
    ...developTask,
    ...integratioTask,
    ...asset
]

export default colRouter

