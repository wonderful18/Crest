<script lang="ts" setup>
import { computed } from 'vue'
import { useEmbedded } from '@/store/modules/embedded'
const embeddedStore = useEmbedded()
const outerUrl = computed(() => {
  return embeddedStore.outerUrl
})
</script>

<template>
  <iframe class="de-jump_outer_url" :src="outerUrl" title="Crest embedded panel"></iframe>
</template>

<style lang="less" scoped>
.de-jump_outer_url {
  width: 100%;
  height: 100%;
  border: 0;
}
</style>
