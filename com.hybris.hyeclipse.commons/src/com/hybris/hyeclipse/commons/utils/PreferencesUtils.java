package com.hybris.hyeclipse.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;

import com.hybris.hyeclipse.commons.Activator;


/**
 * Utility class to work with eclipse preferences mechanism. 
 */
public final class PreferencesUtils {
	
	/**
	 * Private in order to avoid initialization.
	 */
	private PreferencesUtils() { /* empty on purpose */ }
	
	/**
	 * Save object to eclipse store.
	 * 
	 * @param store store that object will be stored in
	 * @param preferenceKey key of preference
	 * @param prefereneValue value of preference 
	 */
	public static void saveObjectToStore(final IPreferenceStore store, final String preferenceKey, final Serializable prefereneValue) {
		store.setValue(preferenceKey, serializeObjectToString(prefereneValue));
	}
	
	/**
	 * Save object as default to eclipse store
	 * 
	 * @param store
	 * @param preferenceKey
	 * @param prefereneValue
	 */
	public static void saveObjectToStoreAsDefault(final IPreferenceStore store, final String preferenceKey, final Serializable prefereneValue) {
		store.setDefault(preferenceKey, serializeObjectToString(prefereneValue));
	}
	
	/**
	 * Read object from store.
	 * 
	 * @param store store from which preference value will be read.
	 * @param preferenceKey key by which preference will be obtained.
	 * @return optional value of searched preference.
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> Optional<T> readObjectFromStore(final IPreferenceStore store, final String preferenceKey) {
		Optional<T> result = Optional.empty();
		final String storedPreference = store.getString(preferenceKey);
		final byte bytes[] = Base64.getDecoder().decode(storedPreference);
		
		try( final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes)) ) {
			result = Optional.of( (T) objectInputStream.readObject());
			return result;
		} catch (ClassNotFoundException | IOException exception) {
			ConsoleUtils.printError(exception.getMessage());
			Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "could not load preferences", exception));
			return Optional.empty();
		}		
	}
		
	/**
	 * Serialize object to string
	 * 
	 * @param object object to serialize
	 * @return String of serialized object
	 */
	public static String serializeObjectToString(final Serializable object) {
		try (final ByteArrayOutputStream byOutputStream = new ByteArrayOutputStream()) {	
			final ObjectOutputStream outputStream = new ObjectOutputStream(byOutputStream);
			
			outputStream.writeObject(object);
			outputStream.flush();
			return Base64.getEncoder().encodeToString(byOutputStream.toByteArray());
		} catch (IOException exception) {
			ConsoleUtils.printError(exception.getMessage());
		} 
		
		return CharactersConstants.EMPTY_STRING;
	}
}
