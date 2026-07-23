<template>
  <tr v-if="priamFrontendUrl">
    <td class="settings-label">
      {{ $t('priamManage') }}
    </td>
    <td class="settings-value" />
    <td class="settings-button">
      <a
        class="edit-link"
        :href="`${priamFrontendUrl}/consent`"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ $t('manageOnPriam') }}
      </a>
    </td>
  </tr>
</template>

<script>
// §4ter: round-trip navigation back to PRIAM, always available (not just
// the forced sign-up redirect) - hidden entirely if PRIAM is not wired up.
export default {
  data () {
    return {
      priamFrontendUrl: import.meta.env.PRIAM_FRONTEND_URL,
    };
  },
};
</script>
