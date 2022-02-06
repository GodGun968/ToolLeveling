package com.tristankechlo.toolleveling.config.values;

import com.google.gson.JsonObject;

public interface IConfigValue<T> {

	String getIdentifier();

	void setToDefault();

	T getValue();

	void serialize(JsonObject jsonObject);

	void deserialize(JsonObject jsonObject);

}
