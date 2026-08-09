<template>
    <div class="app-container" ref="app-container">
        <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick">
            <a-tab-pane key="first" tab="空间成员">
                <User ref="userRef"></User>
            </a-tab-pane>
            <a-tab-pane key="second" tab="空间角色">
                <Role ref="roleRef"></Role>
            </a-tab-pane>
        </a-tabs>
    </div>
</template>

<script setup name="index">
import { ref } from 'vue';
import Role from '@/views/col/setting/spaceUserRel/role.vue';
import User from '@/views/col/setting/spaceUserRel/user.vue';
const activeName = ref('first');
</script>

<style scoped lang="less">
::v-deep(.ant-tabs-nav) {
    margin: 0 0 0px;
    padding: 0px 15px 0px 15px;
    position: relative;
    background-color: #fff;
}
</style>

