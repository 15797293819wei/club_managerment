<template>
  <div class="announcements-page">
    <el-card class="mb16" header="公告列表">
      <div class="toolbar">
        <el-radio-group v-model="mode" size="small" @change="reload">
          <el-radio-button label="visible">全部公告</el-radio-button>
          <el-radio-button label="manage">我发布的公告</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="openPublish">发布公告</el-button>
      </div>

      <el-table :data="records" :loading="loading" size="small" @row-click="openDetail">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="scopeType" label="范围" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.scopeType === 'GLOBAL'" type="danger">系统公告</el-tag>
            <el-tag v-else type="success">社团公告</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clubName" label="所属社团" min-width="160" show-overflow-tooltip />
        <el-table-column prop="publisherName" label="发布人" width="120" />
        <el-table-column prop="createdTime" label="发布时间" width="180">
          <template #default="{ row }">
            {{ row.createdTime?.replace('T', ' ') }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="发布公告" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog.visible" title="公告详情" width="640px">
      <div v-if="detailDialog.record">
        <h3 class="detail-title">{{ detailDialog.record.title }}</h3>
        <p class="detail-meta">
          {{ detailDialog.record.createdTime?.replace('T', ' ') }} ·
          {{ detailDialog.record.publisherName }}
          <span v-if="detailDialog.record.clubName"> · {{ detailDialog.record.clubName }}</span>
        </p>
        <p class="detail-content">
          {{ detailDialog.record.content }}
        </p>
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  listVisibleAnnouncements,
  listManageAnnouncements,
  publishAnnouncement,
  type AnnouncementView
} from '@/api/announcements';

const mode = ref<'visible' | 'manage'>('visible');
const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const records = ref<AnnouncementView[]>([]);

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  title: '',
  content: ''
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
};

const detailDialog = reactive<{
  visible: boolean;
  record: AnnouncementView | null;
}>({
  visible: false,
  record: null
});

async function load() {
  loading.value = true;
  try {
    if (mode.value === 'visible') {
      const res = await listVisibleAnnouncements({ page: page.value, size: size.value });
      records.value = res.records || [];
      total.value = res.total || 0;
    } else {
      const res = await listManageAnnouncements({ page: page.value, size: size.value });
      records.value = res.records || [];
      total.value = res.total || 0;
    }
  } finally {
    loading.value = false;
  }
}

function reload() {
  page.value = 1;
  load();
}

function handlePageChange(p: number) {
  page.value = p;
  load();
}

function handleSizeChange(s: number) {
  size.value = s;
  page.value = 1;
  load();
}

function openPublish() {
  form.title = '';
  form.content = '';
  dialogVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await publishAnnouncement({
      title: form.title.trim(),
      content: form.content.trim()
    });
    ElMessage.success('发布成功');
    dialogVisible.value = false;
    reload();
  } finally {
    saving.value = false;
  }
}

function openDetail(row: AnnouncementView) {
  detailDialog.record = row;
  detailDialog.visible = true;
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.announcements-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
