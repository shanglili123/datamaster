<template>
  <div class="app-container">
    <a-form ref="detailRef" :model="form" :label-col="{ style: { width: '120px' } }">
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="业务分类名称">
            <div class="form-readonly">{{ form.name || "-" }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="英文缩写">
            <div class="form-readonly">{{ form.engName || "-" }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="层级编码">
            <div class="form-readonly">{{ form.code || "-" }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="上级分类">
            <div class="form-readonly">{{ form.parentName || "-" }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="排序">
            <div class="form-readonly">{{ form.sortOrder ?? "-" }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态">
            <dict-tag :options="sys_normal_disable" :value="form.validFlag ? '0' : '1'" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述">
            <div class="form-readonly textarea">{{ form.description || "-" }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="备注">
            <div class="form-readonly textarea">{{ form.remark || "-" }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="创建人">
            <div class="form-readonly">{{ form.createBy || "-" }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="创建时间">
            <div class="form-readonly">{{ parseTime(form.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</div>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup name="BusinessLayerDetail">
import { getBusinessCategory } from "@/api/mdl/businessCategory/businessCategory.js";
import { useRoute } from "vue-router";
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from "vue";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");
const route = useRoute();

const data = reactive({
  form: {}
});
const { form } = toRefs(data);

function getDetail() {
  const id = route.query.id;
  if (id) {
    getBusinessCategory(id).then((response) => {
      form.value = response.data;
    });
  }
}

onMounted(() => {
  getDetail();
});
</script>
