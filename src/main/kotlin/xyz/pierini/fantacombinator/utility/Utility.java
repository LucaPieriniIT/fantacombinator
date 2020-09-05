package xyz.pierini.fantacombinator.utility;

import java.util.Collection;
import java.util.Map;

public class Utility {

	public static final boolean isEmpty(Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof int[]) {
			return ((int[]) obj).length == 0;
		} else if (obj instanceof long[]) {
			return ((long[]) obj).length == 0;
		} else if (obj instanceof boolean[]) {
			return ((boolean[]) obj).length == 0;
		} else if (obj instanceof short[]) {
			return ((short[]) obj).length == 0;
		} else if (obj instanceof char[]) {
			return ((char[]) obj).length == 0;
		} else if (obj instanceof byte[]) {
			return ((byte[]) obj).length == 0;
		} else if (obj instanceof float[]) {
			return ((float[]) obj).length == 0;
		} else if (obj instanceof double[]) {
			return ((double[]) obj).length == 0;
		} else if (obj.getClass().isArray()) {
			return ((Object[]) obj).length == 0;
		} else if (obj instanceof Collection) {
			return ((Collection<?>) obj).isEmpty();
		} else if (obj instanceof Map) {
			return ((Map<?, ?>) obj).isEmpty();
		} else if (obj instanceof String) {
			return "".equals(("" + obj).trim());
		}

		return false;
	}

	public static final boolean isNotEmpty(Object obj) {
		return !isEmpty(obj);
	}
	
}
