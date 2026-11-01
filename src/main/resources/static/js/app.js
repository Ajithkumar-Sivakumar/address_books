const contactForm = document.querySelector("#contact-form");
const contactList = document.querySelector("#contact-list");
const emptyState = document.querySelector("#empty-state");
const contactCount = document.querySelector("#contact-count");
const notice = document.querySelector("#notice");
const searchForm = document.querySelector("#search-form");
const searchInput = document.querySelector("#search-input");
const searchType = document.querySelector("#search-type");
const saveButton = document.querySelector("#save-contact");
const cancelButton = document.querySelector("#cancel-edit");

async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...options.headers,
        },
    });

    if (!response.ok) {
        const errorBody = await response.json().catch(() => null);
        const message = errorBody?.message || `Request failed (${response.status})`;
        const validationDetails = errorBody?.validationErrors
            ? ` ${Object.values(errorBody.validationErrors).join(" ")}`
            : "";
        throw new Error(`${message}${validationDetails}`);
    }

    return response.status === 204 ? null : response.json();
}

function showNotice(message, isError = false) {
    notice.textContent = message;
    notice.classList.toggle("error", isError);
    notice.hidden = false;
}

function clearNotice() {
    notice.hidden = true;
    notice.textContent = "";
    notice.classList.remove("error");
}

function makeCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value || "—";
    return cell;
}

function createContactRow(contact) {
    const row = document.createElement("tr");
    const nameCell = document.createElement("td");
    const name = document.createElement("span");
    name.className = "contact-name";
    name.textContent = `${contact.firstName} ${contact.lastName}`;
    nameCell.append(name);

    const location = [contact.city, contact.country].filter(Boolean).join(", ");
    if (location) {
        const subtitle = document.createElement("span");
        subtitle.className = "contact-subtitle";
        subtitle.textContent = location;
        nameCell.append(subtitle);
    }

    row.append(nameCell, makeCell(contact.email), makeCell(contact.phoneNumber), makeCell(contact.company));

    const actionsCell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";
    actions.append(
        createActionButton("Edit", "edit", contact.id),
        createActionButton("Delete", "delete", contact.id, true),
    );
    actionsCell.append(actions);
    row.append(actionsCell);
    return row;
}

function createActionButton(label, action, id, isDelete = false) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = isDelete ? "row-action delete" : "row-action";
    button.dataset.action = action;
    button.dataset.id = id;
    button.textContent = label;
    button.setAttribute("aria-label", `${label} contact ${id}`);
    return button;
}

function renderContacts(contacts) {
    contactList.replaceChildren(...contacts.map(createContactRow));
    emptyState.hidden = contacts.length > 0;
}

async function refreshContacts() {
    const allContacts = await requestJson("/api/contacts");
    contactCount.textContent = allContacts.length;

    const query = searchInput.value.trim();
    if (!query) {
        renderContacts(allContacts);
        return;
    }

    const parameters = new URLSearchParams({ [searchType.value]: query });
    const matchingContacts = await requestJson(`/api/contacts/search?${parameters}`);
    renderContacts(matchingContacts);
}

function resetForm() {
    contactForm.reset();
    delete contactForm.dataset.contactId;
    document.querySelector("#form-title").textContent = "Add a contact";
    saveButton.textContent = "Save contact";
    cancelButton.hidden = true;
}

function populateForm(contact) {
    for (const [fieldName, fieldValue] of Object.entries(contact)) {
        const field = contactForm.elements.namedItem(fieldName);
        if (field && fieldName !== "id") {
            field.value = fieldValue || "";
        }
    }
    contactForm.dataset.contactId = contact.id;
    document.querySelector("#form-title").textContent = "Edit contact";
    saveButton.textContent = "Save changes";
    cancelButton.hidden = false;
    contactForm.elements.namedItem("firstName").focus();
}

searchForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearNotice();
    try {
        await refreshContacts();
    } catch (error) {
        showNotice(error.message, true);
    }
});

document.querySelector("#clear-search").addEventListener("click", async () => {
    searchForm.reset();
    clearNotice();
    try {
        await refreshContacts();
    } catch (error) {
        showNotice(error.message, true);
    }
});

contactForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearNotice();
    saveButton.disabled = true;

    const values = Object.fromEntries(new FormData(contactForm).entries());
    const payload = Object.fromEntries(
        Object.entries(values).map(([key, value]) => [key, value.trim()]),
    );
    const contactId = contactForm.dataset.contactId;
    const url = contactId ? `/api/contacts/${contactId}` : "/api/contacts";

    try {
        await requestJson(url, {
            method: contactId ? "PUT" : "POST",
            body: JSON.stringify(payload),
        });
        showNotice(contactId ? "Contact updated." : "Contact added.");
        resetForm();
        await refreshContacts();
    } catch (error) {
        showNotice(error.message, true);
    } finally {
        saveButton.disabled = false;
    }
});

cancelButton.addEventListener("click", resetForm);

contactList.addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    clearNotice();
    try {
        if (button.dataset.action === "edit") {
            const contact = await requestJson(`/api/contacts/${button.dataset.id}`);
            populateForm(contact);
            return;
        }

        if (window.confirm("Delete this contact? This action cannot be undone.")) {
            await requestJson(`/api/contacts/${button.dataset.id}`, { method: "DELETE" });
            showNotice("Contact deleted.");
            await refreshContacts();
        }
    } catch (error) {
        showNotice(error.message, true);
    }
});

refreshContacts().catch((error) => showNotice(error.message, true));
