<!-- 复杂详情路由模板
    {
        path: '/example/genStudent',
        component: Layout,
        redirect: 'genStudent',
        hidden: true,
        children: [
            {
                path: 'studentDetail',
                component: () => import('@/views/example/genStudent/detail/index2.vue'),
                name: 'tree',
                meta: { title: '学生详情', activeMenu: '/example/student'  }
            }
        ]
    }
 -->



<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch" style="padding-bottom:15px">
      <div class="infotop" >
        <div class="infotop-title mb15">
          {{ studentDetail.id }}
        </div>
        <a-row :gutter="20">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">ID</div>
              <div class="infotop-row-value">{{ studentDetail.id }}</div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">姓名</div>
              <div class="infotop-row-value">
                {{ studentDetail.name || '-' }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">学生照</div>
              <div class="infotop-row-value">
                <image-preview :src="studentDetail.pictureUrl" :width="50" :height="50"/>
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">教育经历</div>
              <div class="infotop-row-value">
                {{ studentDetail.experience || '-' }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">性别</div>
              <div class="infotop-row-value">
                <dict-tag :options="sys_user_sex" :value="studentDetail.sex "/>
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">年龄</div>
              <div class="infotop-row-value">
                {{ studentDetail.age || '-' }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">学号</div>
              <div class="infotop-row-value">
                {{ studentDetail.studentNumber || '-' }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">班级</div>
              <div class="infotop-row-value">
                {{ studentDetail.grade || '-' }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">爱好</div>
              <div class="infotop-row-value">
                <dict-tag :options="message_level" :value="studentDetail.hobby "/>
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建人</div>
              <div class="infotop-row-value">
                {{ studentDetail.createBy || '-' }}
              </div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建时间</div>
              <div class="infotop-row-value">{{ parseTime(studentDetail.createTime, '{y}-{m}-{d}') }}</div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">备注</div>
              <div class="infotop-row-value">
                {{ studentDetail.remark || '-' }}
              </div>
            </div>
          </a-col>
        </a-row>

      </div>
    </div>

    <div  class="pagecont-bottom">
      <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick">
        <a-tab-pane :tab="'组件一'" key="1">
          <component-one ></component-one>
        </a-tab-pane>
        <a-tab-pane :tab="'组件二'" key="2">
          <component-two ></component-two>
        </a-tab-pane>
      </a-tabs>
    </div>


  </div>
</template>

<script setup name="Student">
import {getStudent } from "@/api/example/genStudent/student";
import { useRoute } from 'vue-router';
import ComponentOne from "@/views/example/genStudent/detail/componentOne.vue";
import ComponentTwo from "@/views/example/genStudent/detail/componentTwo.vue";

const { proxy } = getCurrentInstance();
const { sys_user_sex, message_level } = proxy.useDict('sys_user_sex', 'message_level');

const activeName = ref('1')

const handleClick = (key) => {
  console.log(key)
}

const showSearch = ref(true);
const route = useRoute();
let id = route.query.id || 1;
// 监听 id 变化
watch(
    () => route.query.id,
    (newId) => {
      id = newId || 1;  // 如果 id 为空，使用默认值 1
      getStudentDetailById();

    },
    { immediate: true }  // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);
const data = reactive({
  studentDetail: {
  },
  form: {},
});

const {  studentDetail, rules } = toRefs(data);

/** 复杂详情页面上方表单查询 */
function getStudentDetailById() {
  const _id = id ;
  getStudent(_id).then(response => {
    studentDetail.value = response.data;
    studentDetail.value.hobby = studentDetail.value.hobby.split(",");
  });
}

getStudentDetailById();

</script>

