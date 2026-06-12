import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  assetGovernanceBadges,
  assetTypeOptions,
  canEditGovernance,
  canEditProfile,
  editableGovernanceStatus,
  normalizeGovernanceStatus,
  portalTrustBadges,
  updateGovernanceStatus
} from './dataAssetPolicy.mjs'

describe('data asset governance policy', () => {
  it('keeps dataset governance badges clear and ordered', () => {
    assert.deepEqual(
      assetGovernanceBadges({
        assetType: 'dataset',
        certified: true,
        recommended: true,
        deprecated: true
      }).map(item => item.label),
      ['已废弃']
    )
  })

  it('treats deprecated as an exclusive governance status', () => {
    assert.deepEqual(
      normalizeGovernanceStatus({ certified: '1', recommended: 1, deprecated: true }),
      {
        certified: false,
        recommended: false,
        deprecated: true
      }
    )
  })

  it('allows managed assets to edit profile information', () => {
    assert.equal(canEditProfile({ assetType: 'dataset', canManage: true }), true)
    assert.equal(canEditProfile({ assetType: 'datasource', canManage: true }), true)
    assert.equal(canEditProfile({ assetType: 'table', canManage: true }), true)
    assert.equal(canEditProfile({ assetType: 'dataset', canManage: false }), false)
  })

  it('only allows dataset assets to edit governance status', () => {
    assert.equal(canEditGovernance({ assetType: 'dataset', canManage: true }), true)
    assert.equal(canEditGovernance({ assetType: 'datasource', canManage: true }), false)
    assert.equal(canEditGovernance({ assetType: 'dataset', canManage: false }), false)
  })

  it('normalizes editable dataset governance before opening the form', () => {
    assert.deepEqual(
      editableGovernanceStatus({
        assetType: 'dataset',
        certified: true,
        recommended: true,
        deprecated: true
      }),
      {
        certified: false,
        recommended: false,
        deprecated: true
      }
    )
  })

  it('keeps deprecated mutually exclusive while editing governance', () => {
    assert.deepEqual(
      updateGovernanceStatus(
        { certified: true, recommended: true, deprecated: false },
        'deprecated',
        true
      ),
      {
        certified: false,
        recommended: false,
        deprecated: true
      }
    )

    assert.deepEqual(
      updateGovernanceStatus(
        { certified: false, recommended: false, deprecated: true },
        'recommended',
        true
      ),
      {
        certified: false,
        recommended: true,
        deprecated: false
      }
    )
  })

  it('uses the same trust language in the data portal', () => {
    assert.deepEqual(
      portalTrustBadges({
        certified: true,
        recommended: true,
        deprecated: true
      }).map(item => item.label),
      ['已废弃']
    )
  })

  it('exposes the production asset type set used by the page filter', () => {
    assert.deepEqual(
      assetTypeOptions.map(item => item.value),
      ['all', 'datasource', 'table', 'dataset', 'chart', 'panel', 'screen', 'share']
    )
  })
})
