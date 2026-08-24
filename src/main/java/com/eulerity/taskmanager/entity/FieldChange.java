package com.eulerity.taskmanager.entity;

import java.util.Objects;

public class FieldChange {

	private String fieldName;
	private String oldValue;
	private String newValue;

	public FieldChange() {
	}

	public FieldChange(String fieldName, String oldValue, String newValue) {
		this.fieldName = fieldName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FieldChange other)) {
			return false;
		}
		return Objects.equals(fieldName, other.fieldName)
				&& Objects.equals(oldValue, other.oldValue)
				&& Objects.equals(newValue, other.newValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fieldName, oldValue, newValue);
	}

}
