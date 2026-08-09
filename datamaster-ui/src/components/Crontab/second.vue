<template>
    <a-form>
        <a-form-item>
            <a-radio-group v-model:value="radioValue">
                <a-radio :value="1"> 秒，允许的通配符[, - * /] </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue">
                <a-radio :value="2">
                    周期从
                    <a-input-number v-model:value="cycle01" :min="0" :max="58" /> -
                    <a-input-number v-model:value="cycle02" :min="cycle01 + 1" :max="59" /> 秒
                </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue">
                <a-radio :value="3">
                    从
                    <a-input-number v-model:value="average01" :min="0" :max="58" /> 秒开始，每
                    <a-input-number v-model:value="average02" :min="1" :max="59 - average01" /> 秒执行一次
                </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue">
                <a-radio :value="4">
                    指定
                    <a-select
                        allow-clear
                        v-model:value="checkboxList"
                        placeholder="可多选"
                        mode="multiple"
                        :multiple-limit="10"
                    >
                        <a-select-option v-for="item in 60" :key="item" :label="item - 1" :value="item - 1" />
                    </a-select>
                </a-radio>
            </a-radio-group>
        </a-form-item>
    </a-form>
</template>

<script setup>
    const emit = defineEmits(['update']);
    const props = defineProps({
        cron: {
            type: Object,
            default: {
                second: '0',
                min: '*',
                hour: '*',
                day: '*',
                month: '*',
                week: '?',
                year: ''
            }
        },
        check: {
            type: Function,
            default: () => {}
        }
    });
    const radioValue = ref(4);
    const cycle01 = ref(0);
    const cycle02 = ref(1);
    const average01 = ref(0);
    const average02 = ref(1);
    const checkboxList = ref([0]);
    const checkCopy = ref([0]);
    const cycleTotal = computed(() => {
        cycle01.value = props.check(cycle01.value, 0, 58);
        cycle02.value = props.check(cycle02.value, cycle01.value + 1, 59);
        return cycle01.value + '-' + cycle02.value;
    });
    const averageTotal = computed(() => {
        average01.value = props.check(average01.value, 0, 58);
        average02.value = props.check(average02.value, 1, 59 - average01.value);
        return average01.value + '/' + average02.value;
    });
    const checkboxString = computed(() => {
        return checkboxList.value.join(',');
    });
    watch(
        () => props.cron.second,
        (value) => changeRadioValue(value)
    );
    watch([radioValue, cycleTotal, averageTotal, checkboxString], () => onRadioChange());
    function changeRadioValue(value) {
        if (value === '*') {
            radioValue.value = 1;
        } else if (value.indexOf('-') > -1) {
            const indexArr = value.split('-');
            cycle01.value = Number(indexArr[0]);
            cycle02.value = Number(indexArr[1]);
            radioValue.value = 2;
        } else if (value.indexOf('/') > -1) {
            const indexArr = value.split('/');
            average01.value = Number(indexArr[0]);
            average02.value = Number(indexArr[1]);
            radioValue.value = 3;
        } else {
            checkboxList.value = [...new Set(value.split(',').map((item) => Number(item)))];
            radioValue.value = 4;
        }
    }
    // 单选按钮值变化时
    function onRadioChange() {
        switch (radioValue.value) {
            case 1:
                emit('update', 'second', '*', 'second');
                break;
            case 2:
                emit('update', 'second', cycleTotal.value, 'second');
                break;
            case 3:
                emit('update', 'second', averageTotal.value, 'second');
                break;
            case 4:
                if (checkboxList.value.length === 0) {
                    checkboxList.value.push(checkCopy.value[0]);
                } else {
                    checkCopy.value = checkboxList.value;
                }
                emit('update', 'second', checkboxString.value, 'second');
                break;
        }
    }
</script>

<style lang="scss" scoped>
    .el-input-number--small,
    .el-select,
    .el-select--small {
        margin: 0 0.2rem;
    }
    .el-select,
    .el-select--small {
        width: 18.8rem;
    }
</style>

