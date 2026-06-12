const clone = value => JSON.parse(JSON.stringify(value))

export const createTableCellChangePayload = (form, prop, mergeFieldOptions = []) => {
  const payload = clone(form)

  if (prop === 'mergeCells' && payload.mergeCells && !payload.mergeFields?.length) {
    payload.mergeFields = mergeFieldOptions.map(item => item.id)
  }

  if (prop === 'mergeFields' && !payload.mergeFields?.length) {
    payload.mergeCells = false
    payload.mergeFields = []
  }

  return payload
}
