<template>
  <div class="app-container">
    <el-form ref="detailRef" :model="form" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="业务分类名称">
            <div class="form-readonly">{{ form.name || "-" }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="英文缩写">
            <div class="form-readonly">{{ form.engName || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="层级编码">
            <div class="form-readonly">{{ form.code || "-" }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="上级分类">
            <div class="form-readonly">{{ form.parentName || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="负责人">
            <div class="form-readonly">{{ form.ownerName || "-" }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="负责人电话">
            <div class="form-readonly">{{ form.ownerPhone || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="排序">
            <div class="form-readonly">{{ form.sortOrder ?? "-" }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <dict-tag :options="sys_normal_disable" :value="form.validFlag ? '0' : '1'" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="描述">
            <div class="form-readonly textarea">{{ form.description || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="备注">
            <div class="form-readonly textarea">{{ form.remark || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="创建人">
            <div class="form-readonly">{{ form.createBy || "-" }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="创建时间">
            <div class="form-readonly">{{ parseTime(form.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
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
