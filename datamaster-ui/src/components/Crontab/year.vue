<template>
    <a-form>
        <a-form-item>
            <a-radio-group v-model:value="radioValue" @change="onRadioChange">
                <a-radio :value="1">
                    不填，允许的通配符[, - * /]
                </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue" @change="onRadioChange">
                <a-radio :value="2"> 每年 </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue" @change="onRadioChange">
                <a-radio :value="3">
                    周期从
                    <a-input-number v-model:value="cycle01" :min="fullYear" :max="2098" /> -
                    <a-input-number
                        v-model:value="cycle02"
                        :min="cycle01 ? cycle01 + 1 : fullYear + 1"
                        :max="2099"
                    />
                </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue" @change="onRadioChange">
                <a-radio :value="4">
                    从
                    <a-input-number v-model:value="average01" :min="fullYear" :max="2098" /> 年开始，每
                    <a-input-number v-model:value="average02" :min="1" :max="2099 - average01 || fullYear" />
                    年执行一次
                </a-radio>
            </a-radio-group>
        </a-form-item>

        <a-form-item>
            <a-radio-group v-model:value="radioValue" @change="onRadioChange">
                <a-radio :value="5">
                    指定
                    <a-select
                        allow-clear
                        v-model:value="checkboxList"
                        placeholder="可多选"
                        mode="multiple"
                        :multiple-limit="8"
                    >
                        <a-select-option
                            v-for="item in 9"
                            :key="item"
                            :value="item - 1 + fullYear"
                            :label="item - 1 + fullYear"
                        />
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
    const fullYear = ref(0);
    const maxFullYear = ref(0);
    const radioValue = ref(1);
    const cycle01 = ref(0);
    const cycle02 = ref(0);
    const average01 = ref(0);
    const average02 = ref(1);
    const checkboxList = ref([]);
    const checkCopy = ref([]);
    const cycleTotal = computed(() => {
        console.log('1');

        cycle01.value = props.check(cycle01.value, fullYear.value, maxFullYear.value - 1);
        cycle02.value = props.check(cycle02.value, cycle01.value + 1, maxFullYear.value);
        return cycle01.value + '-' + cycle02.value;
    });
    const averageTotal = computed(() => {
        console.log('2');
        average01.value = props.check(average01.value, fullYear.value, maxFullYear.value - 1);
        average02.value = props.check(average02.value, 1, 10);
        return average01.value + '/' + average02.value;
    });
    const checkboxString = computed(() => {
        console.log('3');
        return checkboxList.value.join(',');
    });
    watch(
        () => props.cron.year,
        (value) => changeRadioValue(value)
    );
    watch([radioValue, cycleTotal, averageTotal, checkboxString], () => onRadioChange());
    function changeRadioValue(value) {
        console.log(value, 'ccccnnn');

        if (value === '') {
            radioValue.value = 1;
        } else if (value === '*') {
            radioValue.value = 2;
        } else if (value.indexOf('-') > -1) {
            const indexArr = value.split('-');
            cycle01.value = Number(indexArr[0]);
            cycle02.value = Number(indexArr[1]);
            radioValue.value = 3;
        } else if (value.indexOf('/') > -1) {
            const indexArr = value.split('/');
            average01.value = Number(indexArr[1]);
            average02.value = Number(indexArr[0]);
            radioValue.value = 4;
        } else {
            checkboxList.value = [...new Set(value.split(',').map((item) => Number(item)))];
            radioValue.value = 5;
        }
    }
    function onRadioChange() {
        console.log(radioValue.value);

        switch (radioValue.value) {
            case 1:
                emit('update', 'year', '', 'year');
                break;
            case 2:
                emit('update', 'year', '*', 'year');
                break;
            case 3:
                emit('update', 'year', cycleTotal.value, 'year');
                break;
            case 4:
                emit('update', 'year', averageTotal.value, 'year');
                break;
            case 5:
                if (checkboxList.value.length === 0) {
                    checkboxList.value.push(checkCopy.value[0]);
                } else {
                    checkCopy.value = checkboxList.value;
                }
                emit('update', 'year', checkboxString.value, 'year');
                break;
        }
    }
    onMounted(() => {
        fullYear.value = Number(new Date().getFullYear());
        maxFullYear.value = fullYear.value + 10;
        cycle01.value = fullYear.value;
        cycle02.value = cycle01.value + 1;
        average01.value = fullYear.value;
        checkCopy.value = [fullYear.value];
    });
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

