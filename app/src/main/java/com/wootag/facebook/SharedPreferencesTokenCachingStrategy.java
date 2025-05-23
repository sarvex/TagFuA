/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.TagFu.facebook.internal.Logger;
import com.TagFu.facebook.internal.Utility;
import com.TagFu.facebook.internal.Validate;

/*
 * <p> An implementation of {@link TokenCachingStrategy TokenCachingStrategy} that uses Android SharedPreferences to
 * persist information. </p> <p> The data to be cached is passed in via a Bundle. Only non-null key-value-pairs where
 * the value is one of the following types (or an array of the same) are persisted: boolean, byte, int, long, float,
 * double, char. In addition, String and List<String> are also supported. </p>
 */
public class SharedPreferencesTokenCachingStrategy extends TokenCachingStrategy {

    private static final String DEFAULT_CACHE_KEY = "com.facebook.SharedPreferencesTokenCachingStrategy.DEFAULT_KEY";
    private static final String TAG = SharedPreferencesTokenCachingStrategy.class.getSimpleName();

    private static final String JSON_VALUE_TYPE = "valueType";
    private static final String JSON_VALUE = "value";
    private static final String JSON_VALUE_ENUM_TYPE = "enumType";

    private static final String TYPE_BOOLEAN = "bool";
    private static final String TYPE_BOOLEAN_ARRAY = "bool[]";
    private static final String TYPE_BYTE = "byte";
    private static final String TYPE_BYTE_ARRAY = "byte[]";
    private static final String TYPE_SHORT = "short";
    private static final String TYPE_SHORT_ARRAY = "short[]";
    private static final String TYPE_INTEGER = "int";
    private static final String TYPE_INTEGER_ARRAY = "int[]";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_LONG_ARRAY = "long[]";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_FLOAT_ARRAY = "float[]";
    private static final String TYPE_DOUBLE = "double";
    private static final String TYPE_DOUBLE_ARRAY = "double[]";
    private static final String TYPE_CHAR = "char";
    private static final String TYPE_CHAR_ARRAY = "char[]";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_STRING_LIST = "stringList";
    private static final String TYPE_ENUM = "enum";

    private final String cacheKey;
    private final SharedPreferences cache;

    /**
     * Creates a default {@link SharedPreferencesTokenCachingStrategy SharedPreferencesTokenCachingStrategy} instance
     * that provides access to a single set of token information.
     *
     * @param context The Context object to use to get the SharedPreferences object.
     * @throws NullPointerException if the passed in Context is null
     */
    public SharedPreferencesTokenCachingStrategy(final Context context) {

        this(context, null);
    }

    /**
     * Creates a {@link SharedPreferencesTokenCachingStrategy SharedPreferencesTokenCachingStrategy} instance that is
     * distinct for the passed in cacheKey.
     *
     * @param context The Context object to use to get the SharedPreferences object.
     * @param cacheKey Identifies a distinct set of token information.
     * @throws NullPointerException if the passed in Context is null
     */
    public SharedPreferencesTokenCachingStrategy(Context context, final String cacheKey) {

        Validate.notNull(context, "context");

        this.cacheKey = Utility.isNullOrEmpty(cacheKey) ? DEFAULT_CACHE_KEY : cacheKey;

        // If the application context is available, use that. However, if it isn't
        // available (possibly because of a context that was created manually), use
        // the passed in context directly.
        final Context applicationContext = context.getApplicationContext();
        context = applicationContext != null ? applicationContext : context;

        this.cache = context.getSharedPreferences(this.cacheKey, Context.MODE_PRIVATE);
    }

    /**
     * Clears out all token information stored in this cache.
     */
    @Override
    public void clear() {

        this.cache.edit().clear().commit();
    }

    /**
     * Returns a Bundle that contains the information stored in this cache
     *
     * @return A Bundle with the information contained in this cache
     */
    @Override
    public Bundle load() {

        final Bundle settings = new Bundle();

        final Map<String, ?> allCachedEntries = this.cache.getAll();

        for (final String key : allCachedEntries.keySet()) {
            try {
                this.deserializeKey(key, settings);
            } catch (final JSONException e) {
                // Error in the cache. So consider it corrupted and return null
                Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, "Error reading cached value for key: '" + key
                        + "' -- " + e);
                return null;
            }
        }

        return settings;
    }

    /**
     * Persists all supported data types present in the passed in Bundle, to the cache
     *
     * @param bundle The Bundle containing information to be cached
     */
    @Override
    public void save(final Bundle bundle) {

        Validate.notNull(bundle, "bundle");

        final SharedPreferences.Editor editor = this.cache.edit();

        for (final String key : bundle.keySet()) {
            try {
                this.serializeKey(key, bundle, editor);
            } catch (final JSONException e) {
                // Error in the bundle. Don't store a partial cache.
                Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, "Error processing value for key: '" + key + "' -- "
                        + e);

                // Bypass the commit and just return. This cancels the entire edit transaction
                return;
            }
        }

        final boolean successfulCommit = editor.commit();
        if (!successfulCommit) {
            Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, "SharedPreferences.Editor.commit() was not successful");
        }
    }

    private void deserializeKey(final String key, final Bundle bundle) throws JSONException {

        final String jsonString = this.cache.getString(key, "{}");
        final JSONObject json = new JSONObject(jsonString);

        final String valueType = json.getString(JSON_VALUE_TYPE);

        if (valueType.equals(TYPE_BOOLEAN)) {
            bundle.putBoolean(key, json.getBoolean(JSON_VALUE));
        } else if (valueType.equals(TYPE_BOOLEAN_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final boolean[] array = new boolean[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = jsonArray.getBoolean(i);
            }
            bundle.putBooleanArray(key, array);
        } else if (valueType.equals(TYPE_BYTE)) {
            bundle.putByte(key, (byte) json.getInt(JSON_VALUE));
        } else if (valueType.equals(TYPE_BYTE_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final byte[] array = new byte[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = (byte) jsonArray.getInt(i);
            }
            bundle.putByteArray(key, array);
        } else if (valueType.equals(TYPE_SHORT)) {
            bundle.putShort(key, (short) json.getInt(JSON_VALUE));
        } else if (valueType.equals(TYPE_SHORT_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final short[] array = new short[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = (short) jsonArray.getInt(i);
            }
            bundle.putShortArray(key, array);
        } else if (valueType.equals(TYPE_INTEGER)) {
            bundle.putInt(key, json.getInt(JSON_VALUE));
        } else if (valueType.equals(TYPE_INTEGER_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final int[] array = new int[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = jsonArray.getInt(i);
            }
            bundle.putIntArray(key, array);
        } else if (valueType.equals(TYPE_LONG)) {
            bundle.putLong(key, json.getLong(JSON_VALUE));
        } else if (valueType.equals(TYPE_LONG_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final long[] array = new long[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = jsonArray.getLong(i);
            }
            bundle.putLongArray(key, array);
        } else if (valueType.equals(TYPE_FLOAT)) {
            bundle.putFloat(key, (float) json.getDouble(JSON_VALUE));
        } else if (valueType.equals(TYPE_FLOAT_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final float[] array = new float[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = (float) jsonArray.getDouble(i);
            }
            bundle.putFloatArray(key, array);
        } else if (valueType.equals(TYPE_DOUBLE)) {
            bundle.putDouble(key, json.getDouble(JSON_VALUE));
        } else if (valueType.equals(TYPE_DOUBLE_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final double[] array = new double[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                array[i] = jsonArray.getDouble(i);
            }
            bundle.putDoubleArray(key, array);
        } else if (valueType.equals(TYPE_CHAR)) {
            final String charString = json.getString(JSON_VALUE);
            if ((charString != null) && (charString.length() == 1)) {
                bundle.putChar(key, charString.charAt(0));
            }
        } else if (valueType.equals(TYPE_CHAR_ARRAY)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final char[] array = new char[jsonArray.length()];
            for (int i = 0; i < array.length; i++) {
                final String charString = jsonArray.getString(i);
                if ((charString != null) && (charString.length() == 1)) {
                    array[i] = charString.charAt(0);
                }
            }
            bundle.putCharArray(key, array);
        } else if (valueType.equals(TYPE_STRING)) {
            bundle.putString(key, json.getString(JSON_VALUE));
        } else if (valueType.equals(TYPE_STRING_LIST)) {
            final JSONArray jsonArray = json.getJSONArray(JSON_VALUE);
            final int numStrings = jsonArray.length();
            final ArrayList<String> stringList = new ArrayList<String>(numStrings);
            for (int i = 0; i < numStrings; i++) {
                final Object jsonStringValue = jsonArray.get(i);
                stringList.add(i, jsonStringValue == JSONObject.NULL ? null : (String) jsonStringValue);
            }
            bundle.putStringArrayList(key, stringList);
        } else if (valueType.equals(TYPE_ENUM)) {
            try {
                final String enumType = json.getString(JSON_VALUE_ENUM_TYPE);
                @SuppressWarnings({ "unchecked", "rawtypes" })
                final Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(enumType);
                @SuppressWarnings("unchecked")
                final Enum<?> enumValue = Enum.valueOf(enumClass, json.getString(JSON_VALUE));
                bundle.putSerializable(key, enumValue);
            } catch (final ClassNotFoundException e) {
            } catch (final IllegalArgumentException e) {
            }
        }
    }

    private void serializeKey(final String key, final Bundle bundle, final SharedPreferences.Editor editor)
            throws JSONException {

        final Object value = bundle.get(key);
        if (value == null) {
            // Cannot serialize null values.
            return;
        }

        String supportedType = null;
        JSONArray jsonArray = null;
        final JSONObject json = new JSONObject();

        if (value instanceof Byte) {
            supportedType = TYPE_BYTE;
            json.put(JSON_VALUE, ((Byte) value).intValue());
        } else if (value instanceof Short) {
            supportedType = TYPE_SHORT;
            json.put(JSON_VALUE, ((Short) value).intValue());
        } else if (value instanceof Integer) {
            supportedType = TYPE_INTEGER;
            json.put(JSON_VALUE, ((Integer) value).intValue());
        } else if (value instanceof Long) {
            supportedType = TYPE_LONG;
            json.put(JSON_VALUE, ((Long) value).longValue());
        } else if (value instanceof Float) {
            supportedType = TYPE_FLOAT;
            json.put(JSON_VALUE, ((Float) value).doubleValue());
        } else if (value instanceof Double) {
            supportedType = TYPE_DOUBLE;
            json.put(JSON_VALUE, ((Double) value).doubleValue());
        } else if (value instanceof Boolean) {
            supportedType = TYPE_BOOLEAN;
            json.put(JSON_VALUE, ((Boolean) value).booleanValue());
        } else if (value instanceof Character) {
            supportedType = TYPE_CHAR;
            json.put(JSON_VALUE, value.toString());
        } else if (value instanceof String) {
            supportedType = TYPE_STRING;
            json.put(JSON_VALUE, value);
        } else if (value instanceof Enum<?>) {
            supportedType = TYPE_ENUM;
            json.put(JSON_VALUE, value.toString());
            json.put(JSON_VALUE_ENUM_TYPE, value.getClass().getName());
        } else {
            // Optimistically create a JSONArray. If not an array type, we can null
            // it out later
            jsonArray = new JSONArray();
            if (value instanceof byte[]) {
                supportedType = TYPE_BYTE_ARRAY;
                for (final byte v : (byte[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof short[]) {
                supportedType = TYPE_SHORT_ARRAY;
                for (final short v : (short[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof int[]) {
                supportedType = TYPE_INTEGER_ARRAY;
                for (final int v : (int[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof long[]) {
                supportedType = TYPE_LONG_ARRAY;
                for (final long v : (long[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof float[]) {
                supportedType = TYPE_FLOAT_ARRAY;
                for (final float v : (float[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof double[]) {
                supportedType = TYPE_DOUBLE_ARRAY;
                for (final double v : (double[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof boolean[]) {
                supportedType = TYPE_BOOLEAN_ARRAY;
                for (final boolean v : (boolean[]) value) {
                    jsonArray.put(v);
                }
            } else if (value instanceof char[]) {
                supportedType = TYPE_CHAR_ARRAY;
                for (final char v : (char[]) value) {
                    jsonArray.put(String.valueOf(v));
                }
            } else if (value instanceof List<?>) {
                supportedType = TYPE_STRING_LIST;
                @SuppressWarnings("unchecked")
                final List<String> stringList = (List<String>) value;
                for (final String v : stringList) {
                    jsonArray.put((v == null) ? JSONObject.NULL : v);
                }
            } else {
                // Unsupported type. Clear out the array as a precaution even though
                // it is redundant with the null supportedType.
                jsonArray = null;
            }
        }

        if (supportedType != null) {
            json.put(JSON_VALUE_TYPE, supportedType);
            if (jsonArray != null) {
                // If we have an array, it has already been converted to JSON. So use
                // that instead.
                json.putOpt(JSON_VALUE, jsonArray);
            }

            final String jsonString = json.toString();
            editor.putString(key, jsonString);
        }
    }
}
