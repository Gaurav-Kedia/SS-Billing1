(function () {
    const GST_RATE = 0.18;

    const tableBody = document.querySelector('#itemsTable tbody');
    const addButton = document.querySelector('#addItem');
    const discountInput = document.querySelector('[name="discountAmount"]');
    const totalBaseEl = document.querySelector('#totalBase');
    const totalTaxEl = document.querySelector('#totalTax');
    const grandTotalEl = document.querySelector('#grandTotal');
    const issueDateInput = document.querySelector('input[name="issueDate"]');
    const deliveryDateInput = document.querySelector('input[name="deliveryDate"]');

    function ensureDefaultDate(field) {
        if (!field) {
            return;
        }
        if (!field.value) {
            const today = new Date();
            const month = String(today.getMonth() + 1).padStart(2, '0');
            const day = String(today.getDate()).padStart(2, '0');
            field.value = `${today.getFullYear()}-${month}-${day}`;
        }
    }

    ensureDefaultDate(issueDateInput);
    ensureDefaultDate(deliveryDateInput);

    if (!tableBody) {
        return;
    }

    const hsnDefaults = buildHsnDefaults();

    function buildHsnDefaults() {
        const map = {};
        const select = document.querySelector('.hsn-select');
        if (!select) {
            return map;
        }
        Array.from(select.options).forEach(option => {
            if (option.value && option.textContent.includes(' - ')) {
                const parts = option.textContent.split(' - ');
                if (parts.length === 2) {
                    map[option.value] = parts[1];
                }
            }
        });
        return map;
    }

    function formatCurrency(value) {
        if (isNaN(value)) {
            return '';
        }
        return value.toFixed(2);
    }

    function parseNumber(value, fallback = 0) {
        const number = parseFloat(value);
        return isNaN(number) ? fallback : number;
    }

    function formatFromPaise(paise) {
        return (paise / 100).toFixed(2);
    }

    function updateRowCalculations(row) {
        const finalInput = row.querySelector('.final-amount');
        const quantityInput = row.querySelector('.quantity');
        const baseInput = row.querySelector('.base-amount');
        const sgstInput = row.querySelector('.sgst-amount');
        const cgstInput = row.querySelector('.cgst-amount');

        const finalAmountPerUnit = parseNumber(finalInput.value);
        const quantity = Math.max(parseNumber(quantityInput.value, 1), 1);
        if (finalAmountPerUnit <= 0) {
            baseInput.value = '';
            sgstInput.value = '';
            cgstInput.value = '';
            return;
        }

        const finalPaisePerUnit = Math.round(finalAmountPerUnit * 100);
        const totalFinalPaise = finalPaisePerUnit * quantity;
        const basePaise = Math.round(totalFinalPaise / (1 + GST_RATE));
        const taxPaise = totalFinalPaise - basePaise;
        const sgstPaise = Math.round(taxPaise / 2);
        const cgstPaise = taxPaise - sgstPaise;

        baseInput.value = formatFromPaise(basePaise);
        sgstInput.value = formatFromPaise(sgstPaise);
        cgstInput.value = formatFromPaise(cgstPaise);
    }

    function updateTotals() {
        let totalBase = 0;
        let totalTax = 0;
        let totalFinal = 0;

        tableBody.querySelectorAll('tr').forEach(row => {
            const base = parseNumber(row.querySelector('.base-amount').value);
            const sgst = parseNumber(row.querySelector('.sgst-amount').value);
            const cgst = parseNumber(row.querySelector('.cgst-amount').value);
            const finalAmountPerUnit = parseNumber(row.querySelector('.final-amount').value);
            const quantity = Math.max(parseNumber(row.querySelector('.quantity').value, 1), 1);
            totalBase += base;
            totalTax += sgst + cgst;
            totalFinal += finalAmountPerUnit * quantity;
        });

        const discount = parseNumber(discountInput.value);
        const grandTotal = Math.max(totalFinal - discount, 0);

        totalBaseEl.value = formatCurrency(totalBase);
        totalTaxEl.value = formatCurrency(totalTax);
        grandTotalEl.value = formatCurrency(grandTotal);
    }

    function attachRowEvents(row) {
        const finalInput = row.querySelector('.final-amount');
        const quantityInput = row.querySelector('.quantity');
        const hsnSelect = row.querySelector('.hsn-select');
        const removeButton = row.querySelector('.remove-row');
        const descriptionInput = row.querySelector('input[name$=".description"]');

        finalInput.addEventListener('input', () => {
            updateRowCalculations(row);
            updateTotals();
        });
        quantityInput.addEventListener('input', () => {
            if (quantityInput.value === '' || parseInt(quantityInput.value, 10) < 1) {
                quantityInput.value = '1';
            }
            updateRowCalculations(row);
            updateTotals();
        });
        hsnSelect.addEventListener('change', () => {
            const defaultDescription = hsnDefaults[hsnSelect.value];
            if (descriptionInput && (!descriptionInput.value || descriptionInput.dataset.default === 'true')) {
                descriptionInput.value = defaultDescription || '';
                descriptionInput.dataset.default = defaultDescription ? 'true' : 'false';
            }
        });
        if (descriptionInput) {
            descriptionInput.addEventListener('input', () => {
                descriptionInput.dataset.default = descriptionInput.value ? 'false' : 'true';
            });
            descriptionInput.dataset.default = descriptionInput.value ? 'false' : 'true';
        }
        removeButton.addEventListener('click', () => {
            if (tableBody.querySelectorAll('tr').length === 1) {
                row.querySelectorAll('input').forEach(input => {
                    if (input.classList.contains('quantity')) {
                        input.value = '1';
                    } else if (!input.readOnly) {
                        input.value = '';
                    } else {
                        input.value = '';
                    }
                });
                row.querySelector('.hsn-select').selectedIndex = 0;
            } else {
                row.remove();
                renumberRows();
                updateTotals();
            }
        });
    }

    function renumberRows() {
        tableBody.querySelectorAll('tr').forEach((row, index) => {
            row.dataset.index = index;
            const headerCell = row.querySelector('td:first-child');
            headerCell.textContent = index + 1;
            row.querySelectorAll('input, select').forEach(field => {
                if (field.name) {
                    field.name = field.name.replace(/items\[[0-9]+\]/, `items[${index}]`);
                }
                if (field.id) {
                    field.id = field.id.replace(/items_[0-9]+_/, `items_${index}_`);
                }
            });
        });
    }

    function addNewRow() {
        const lastRow = tableBody.querySelector('tr:last-child');
        const clone = lastRow.cloneNode(true);
        clone.querySelectorAll('input').forEach(input => {
            if (input.classList.contains('base-amount') || input.classList.contains('sgst-amount') || input.classList.contains('cgst-amount')) {
                input.value = '';
            } else if (input.classList.contains('final-amount')) {
                input.value = '';
            } else if (input.classList.contains('quantity')) {
                input.value = '1';
            } else {
                input.value = '';
            }
        });
        const select = clone.querySelector('.hsn-select');
        if (select) {
            select.selectedIndex = 0;
        }
        tableBody.appendChild(clone);
        renumberRows();
        attachRowEvents(clone);
        updateRowCalculations(clone);
        updateTotals();
    }

    tableBody.querySelectorAll('tr').forEach(row => {
        attachRowEvents(row);
        updateRowCalculations(row);
    });
    updateTotals();

    addButton.addEventListener('click', addNewRow);
    discountInput.addEventListener('input', updateTotals);
})();
