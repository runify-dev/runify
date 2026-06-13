<template>
  <div class="p-4">
    <!-- 顶部工具栏 -->
    <div class="flex items-center justify-between gap-4 mb-6">
      <div class="flex items-center gap-3">
        <Button icon="pi pi-arrow-left" severity="secondary" variant="text" @click="router.back()"/>
        <h1 class="text-xl font-semibold">{{ t('skill.store.title') }}</h1>
      </div>
      <InputGroup class="max-w-sm">
        <InputGroupAddon>
          <i class="pi pi-search"/>
        </InputGroupAddon>
        <InputText v-model="searchText" :placeholder="t('skill.store.search')"/>
      </InputGroup>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <ProgressSpinner/>
    </div>

    <!-- 技能列表 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <template v-for="item in filteredList" :key="item.id">
        <Card
          class="group cursor-pointer hover:-translate-y-0.5 transition-all duration-200"
          :pt="{
            root: {
              class: 'h-full flex flex-col',
              style: 'border: 1px solid var(--p-content-border-color); box-shadow: var(--p-shadow-1); background: var(--p-content-background);'
            },
            body: { class: 'flex-1 flex flex-col gap-3' },
            content: { class: 'flex-1' }
          }"
          @click="openDetail(item)"
        >
          <template #header>
            <div class="flex items-start justify-between p-4 pb-0">
              <div class="w-12 h-12 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-lg shrink-0 overflow-hidden">
                <img
                  v-if="item.iconUrl"
                  :src="item.iconUrl"
                  alt="icon"
                  class="w-full h-full object-cover"
                  @error="($event.target as HTMLImageElement).style.display='none'"
                />
                <i v-else class="pi pi-bolt"/>
              </div>
              <Tag v-if="item.versions.length > 0" :value="item.versions[0].version" severity="info" class="text-xs"/>
            </div>
          </template>
          <template #content>
            <h3 class="text-sm font-semibold text-surface-900 truncate mb-1">{{ item.name }}</h3>
            <p class="text-xs text-surface-500 leading-relaxed line-clamp-3 min-h-[3rem]">{{ item.summary }}</p>
          </template>
          <template #footer>
            <div class="flex items-center justify-between pt-2.5 border-t" style="border-color: var(--p-content-border-color);">
              <div class="flex flex-wrap gap-1.5">
                <Tag v-if="getLatestUpgradeVersion(item.id)" :value="t('skill.store.upgrade')" severity="success" class="text-[11px]"/>
                <Tag v-else-if="installedSkills[item.id]" :value="t('skill.store.installed')" severity="info" class="text-[11px]"/>
                <Tag v-for="tag in item.tags.slice(0, installedSkills[item.id] || getLatestUpgradeVersion(item.id) ? 2 : 3)" :key="tag" :value="tag" severity="secondary" class="text-[11px]"/>
              </div>
              <span class="text-[11px] text-surface-400">{{ item.versions[0]?.version }}</span>
            </div>
          </template>
        </Card>
      </template>

      <!-- 空状态 -->
      <div v-if="filteredList.length === 0" class="col-span-full flex flex-col items-center justify-center py-16 text-surface-400">
        <i class="pi pi-inbox text-5xl mb-4 opacity-40"/>
        <p class="text-sm">{{ searchText ? t('skill.store.noResults') : t('skill.store.empty') }}</p>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <Dialog v-model:visible="detailVisible" :style="{ width: '720px' }" :header="selectedSkill?.name || ''" modal>
      <div v-if="selectedSkill" class="flex flex-col gap-5">
        <!-- 基本信息 -->
        <div class="flex items-start gap-4">
          <div class="w-16 h-16 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500 text-2xl shrink-0 overflow-hidden">
            <img
              v-if="selectedSkill.iconUrl"
              :src="selectedSkill.iconUrl"
              alt="icon"
              class="w-full h-full object-cover"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <i v-else class="pi pi-bolt"/>
          </div>
          <div class="flex-1 min-w-0">
            <h2 class="text-lg font-semibold mb-1">{{ selectedSkill.name }}</h2>
            <p class="text-sm text-surface-500">{{ selectedSkill.summary }}</p>
          </div>
        </div>

        <!-- 标签 -->
        <div class="flex flex-wrap gap-2">
          <Tag v-for="tag in selectedSkill.tags" :key="tag" :value="tag"/>
        </div>

        <!-- 能力 -->
        <div v-if="selectedSkill.capabilities.length > 0">
          <h4 class="text-sm font-medium mb-2">{{ t('skill.store.capabilities') }}</h4>
          <div class="flex flex-wrap gap-2">
            <span v-for="cap in selectedSkill.capabilities" :key="cap"
              class="text-xs px-2 py-1 rounded-full bg-surface-100 text-surface-600">
              {{ cap }}
            </span>
          </div>
        </div>

        <!-- 依赖 -->
        <div v-if="selectedSkill.requires.length > 0">
          <h4 class="text-sm font-medium mb-2">{{ t('skill.store.requires') }}</h4>
          <div class="flex flex-wrap gap-2">
            <span v-for="req in selectedSkill.requires" :key="req"
              class="text-xs px-2 py-1 rounded-full bg-orange-50 text-orange-600">
              {{ req }}
            </span>
          </div>
        </div>

        <!-- 备注 -->
        <div v-if="selectedSkill.note" class="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
          <p class="text-xs text-blue-600 dark:text-blue-300">{{ selectedSkill.note }}</p>
        </div>

        <!-- 版本历史 -->
        <div>
          <h4 class="text-sm font-medium mb-3">{{ t('skill.store.versionHistory') }}</h4>

          <!-- 版本加载中 -->
          <div v-if="loadingVersions" class="flex items-center justify-center py-4">
            <ProgressSpinner style="width: 24px; height: 24px"/>
          </div>

          <!-- 版本列表 -->
          <div v-else-if="versionDetails.length > 0" class="space-y-3">
            <div v-for="ver in versionDetails" :key="ver.version"
              class="p-4 rounded-lg border"
              style="border-color: var(--p-content-border-color);"
            >
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-2">
                  <span class="font-semibold text-sm">{{ ver.version }}</span>
                  <Tag v-if="ver.prerelease" value="Pre-release" severity="warn" class="text-[10px]"/>
                  <span class="text-xs text-surface-400">{{ formatDate(ver.time) }}</span>
                </div>
                <div v-if="getInstallStatus(selectedSkill.id, ver.version) === 'installed'">
                  <Tag :value="t('skill.store.installed')" severity="success" class="text-xs"/>
                </div>
                <Button v-else-if="getInstallStatus(selectedSkill.id, ver.version) === 'upgrade'"
                  :label="t('skill.store.upgrade')"
                  size="small"
                  severity="success"
                  :loading="installingVersion === ver.version"
                  @click="installVersion(selectedSkill, ver.version)"
                />
                <Button v-else
                  :label="t('skill.store.install')"
                  size="small"
                  :loading="installingVersion === ver.version"
                  @click="installVersion(selectedSkill, ver.version)"
                />
              </div>
              <p v-if="ver.title" class="text-sm font-medium mb-2">{{ ver.title }}</p>
              <ul v-if="ver.highlights?.length > 0" class="space-y-1">
                <li v-for="(h, i) in ver.highlights" :key="i" class="text-xs text-surface-500 flex items-start gap-1.5">
                  <span class="text-surface-300 mt-0.5">•</span>
                  <span>{{ h }}</span>
                </li>
              </ul>
            </div>
          </div>

          <!-- 无版本详情时显示基本信息 -->
          <div v-else class="space-y-3">
            <div v-for="ver in selectedSkill.versions" :key="ver.version"
              class="p-4 rounded-lg border"
              style="border-color: var(--p-content-border-color);"
            >
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="font-semibold text-sm">{{ ver.version }}</span>
                  <span class="text-xs text-surface-400">{{ formatDate(ver.time) }}</span>
                </div>
                <div v-if="getInstallStatus(selectedSkill.id, ver.version) === 'installed'">
                  <Tag :value="t('skill.store.installed')" severity="success" class="text-xs"/>
                </div>
                <Button v-else-if="getInstallStatus(selectedSkill.id, ver.version) === 'upgrade'"
                  :label="t('skill.store.upgrade')"
                  size="small"
                  severity="success"
                  :loading="installingVersion === ver.version"
                  @click="installVersion(selectedSkill, ver.version)"
                />
                <Button v-else
                  :label="t('skill.store.install')"
                  size="small"
                  :loading="installingVersion === ver.version"
                  @click="installVersion(selectedSkill, ver.version)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { t } from '@/locales'
import skillStoreApi from '@/api/skill-store'
import type { SkillStoreItem, SkillVersionDetail } from '@/api/skill-store'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { TreeCommonAPI } from '@/api/tree'
import type { Node } from '@/api/type/node'

interface SkillWithIcon extends SkillStoreItem {
  iconUrl: string
}

const router = useRouter()
const toast = useToast()
const treeCommonAPI = new TreeCommonAPI('skill')

const loading = ref(true)
const searchText = ref('')
const skills = ref<SkillWithIcon[]>([])
const detailVisible = ref(false)
const selectedSkill = ref<SkillWithIcon | null>(null)
const installingVersion = ref<string | null>(null)
const loadingVersions = ref(false)
const versionDetails = ref<SkillVersionDetail[]>([])
const installedSkills = ref<Record<string, { id: string, version: string }>>({})

const filteredList = computed(() =>
  searchText.value
    ? skills.value.filter(s =>
        s.name.includes(searchText.value) ||
        s.summary.includes(searchText.value) ||
        s.tags.some(t => t.includes(searchText.value))
      )
    : skills.value
)

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr).toLocaleDateString()
  } catch {
    return dateStr
  }
}

const loadVersionDetails = async (skill: SkillWithIcon) => {
  loadingVersions.value = true
  versionDetails.value = []
  try {
    const details = await Promise.allSettled(
      skill.versions.map(v => skillStoreApi.getSkillVersionDetail(skill.id, v.version))
    )
    versionDetails.value = details
      .filter((r): r is PromiseFulfilledResult<SkillVersionDetail> => r.status === 'fulfilled')
      .map(r => r.value)
  } catch (e) {
    console.error('Failed to load version details:', e)
  } finally {
    loadingVersions.value = false
  }
}

const openDetail = (skill: SkillWithIcon) => {
  selectedSkill.value = skill
  detailVisible.value = true
  loadVersionDetails(skill)
}

const handleClick = (skill: SkillWithIcon) => {
  const installed = installedSkills.value[skill.id]
  if (installed) {
    router.push({ name: 'skillSetting', params: { id: installed.id } })
  } else {
    openDetail(skill)
  }
}

const installVersion = async (skill: SkillWithIcon, version: string) => {
  installingVersion.value = version
  try {
    const zipUrl = skillStoreApi.getSkillZipUrl(skill.id, version)
    await skillStoreApi.installFromStore(ROOT_FOLDER_ID, skill.id, version, zipUrl)
    toast.add({ severity: 'success', summary: t('skill.store.installSuccess'), life: 2000 })
    detailVisible.value = false
  } catch (e) {
    console.error('Install failed:', e)
    toast.add({ severity: 'error', summary: t('skill.store.installFailed'), life: 3000 })
  } finally {
    installingVersion.value = null
  }
}

const getInstallStatus = (skillId: string, version: string): 'installed' | 'upgrade' | 'install' => {
  const installed = installedSkills.value[skillId]
  if (!installed) return 'install'
  if (installed.version === version) return 'installed'
  // Check if this version is newer than installed
  const skill = skills.value.find(s => s.id === skillId)
  if (skill) {
    const installedIdx = skill.versions.findIndex(v => v.version === installed.version)
    const currentIdx = skill.versions.findIndex(v => v.version === version)
    if (currentIdx < installedIdx) return 'upgrade' // versions are sorted newest first
  }
  return 'installed'
}

const getLatestUpgradeVersion = (skillId: string): string | null => {
  const installed = installedSkills.value[skillId]
  if (!installed) return null
  const skill = skills.value.find(s => s.id === skillId)
  if (!skill) return null
  const latestVersion = skill.versions[0]
  if (latestVersion && latestVersion.version !== installed.version) {
    return latestVersion.version
  }
  return null
}

onMounted(async () => {
  try {
    // Load installed skills
    const res = await treeCommonAPI.listResource(ROOT_FOLDER_ID)
    const nodes: Node[] = res.data || []
    const map: Record<string, { id: string, version: string }> = {}
    for (const node of nodes) {
      const meta = node.meta as any
      if (meta?.storeId) {
        map[meta.storeId] = { id: node.id, version: meta.storeVersion }
      }
    }
    installedSkills.value = map

    // Load store skills
    const index = await skillStoreApi.getSkillStoreIndex()
    const skillDetails = await Promise.all(
      index.skills.map(id => skillStoreApi.getSkillDetail(id))
    )
    skills.value = skillDetails.map(s => ({
      ...s,
      iconUrl: s.versions.length > 0
        ? skillStoreApi.getSkillIconUrl(s.id, s.versions[0].version)
        : ''
    }))
  } catch (e) {
    console.error('Failed to load skill store:', e)
    toast.add({ severity: 'error', summary: t('skill.store.loadFailed'), life: 3000 })
  } finally {
    loading.value = false
  }
})
</script>
