<template>
  <a v-if="isExt" v-bind="linkProps()">
    <slot />
  </a>
  <router-link v-else :to="to" custom v-slot="{ href, navigate }">
    <a :href="href" @click="handleInternalClick($event, navigate)">
      <slot />
    </a>
  </router-link>
</template>

<script setup>
import { isExternal } from '@/utils/validate'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  to: {
    type: [String, Object],
    required: true
  }
})

const route = useRoute()
const router = useRouter()

const isExt = computed(() => {
  return isExternal(props.to)
})

function linkProps() {
  return {
    href: props.to,
    target: '_blank',
    rel: 'noopener'
  }
}

function handleInternalClick(event, navigate) {
  const target = router.resolve(props.to)
  if (target.fullPath === route.fullPath) {
    event.preventDefault()
    router.replace({ path: `/redirect${route.fullPath}` })
    return
  }
  navigate(event)
}
</script>
