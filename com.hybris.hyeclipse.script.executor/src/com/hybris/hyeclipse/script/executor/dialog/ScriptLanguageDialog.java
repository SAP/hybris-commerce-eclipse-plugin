package com.hybris.hyeclipse.script.executor.dialog;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jsoup.helper.StringUtil;

import com.hybris.hyeclipse.commons.utils.CharactersConstants;
import com.hybris.hyeclipse.script.executor.preferences.Messages;

/**
 * Class responsible for logic of add and modification of a script language.
 */
public class ScriptLanguageDialog extends TitleAreaDialog {

	/**
	 * Determinate whether dialog is open in modification or creation mode.
	 */
	private final boolean isModification;

	/**
	 * Map of already specified script languages
	 */
	private final Map<String, String> scriptLanguages;

	/* Text inputs */
	private Text scriptLanguageText;
	private Text scriptLanguageFileExtensionText;

	/* Variables */
	private String scriptLanguageName;
	private String oldScriptLanguageName;
	private String scriptLanguageFileExtension;

	/**
	 * Dialog title
	 */
	private final String title;

	/**
	 * Constructor for dialog to add new script language
	 * 
	 * @param parentShell
	 *            shell object
	 * @param scriptLanguages
	 *            map of already existing script languages
	 */
	public ScriptLanguageDialog(final Shell parentShell, final Map<String, String> scriptLanguages) {
		super(parentShell);

		this.isModification = false;
		this.scriptLanguages = scriptLanguages;
		this.title = Messages.ScriptLanguageDialog_Add_Lang;
	}

	/**
	 * Constructor for dialog to modify existing script language
	 * 
	 * @param parentShell
	 *            shell object
	 * @param scriptLanguages
	 *            map of already existing script languages
	 * @param scriptLanguageName
	 *            name of a script to modify
	 */
	public ScriptLanguageDialog(final Shell parentShell, final Map<String, String> scriptLanguages,
			final String scriptLanguageName) {
		super(parentShell);

		this.isModification = true;
		this.scriptLanguages = scriptLanguages;
		this.scriptLanguageName = scriptLanguageName;
		this.oldScriptLanguageName = scriptLanguageName;
		this.title = Messages.ScriptLanguageDialog_Modify_Script_Lang;
		this.scriptLanguageFileExtension = scriptLanguages.get(scriptLanguageName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void create() {
		super.create();
		setTitle(title);
		setMessage(CharactersConstants.EMPTY_STRING);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);
		final Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		scriptLanguageText = createText(container, Messages.ScriptLanguageDialog_Script_Lang);
		scriptLanguageFileExtensionText = createText(container, Messages.ScriptLanguageDialog_File_Extension);

		if (!StringUtil.isBlank(scriptLanguageName)) {
			scriptLanguageText.setText(scriptLanguageName);
		}

		if (!StringUtil.isBlank(scriptLanguageFileExtension)) {
			scriptLanguageFileExtensionText.setText(scriptLanguageFileExtension);
		}

		scriptLanguageText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				final Text input = (Text) event.getSource();

				setScriptLanguageName(input.getText());
				validateScript(getScriptLanguageName(), getScriptLanguageFileExtension());
			}
		});

		scriptLanguageFileExtensionText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				final Text input = (Text) event.getSource();

				setScriptLanguageFileExtension(input.getText());
				validateScript(getScriptLanguageName(), getScriptLanguageFileExtension());
			}
		});

		return area;
	}

	/**
	 * Creates Text input
	 * 
	 * @param container
	 *            container in which text input will be created
	 * @param labelText
	 *            label text of a input
	 * @return text input object
	 */
	private Text createText(final Composite container, final String labelText) {
		final Label label = new Label(container, SWT.NONE);
		label.setText(labelText);

		final GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;

		final Text text = new Text(container, SWT.BORDER);
		text.setLayoutData(data);

		return text;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Save content of a text fields because they get disposed once dialog is
	 * closed.
	 */
	private void saveInput() {
		scriptLanguageName = scriptLanguageText.getText();
		scriptLanguageFileExtension = scriptLanguageFileExtensionText.getText();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

	/**
	 * Validate correctness of script language data
	 * 
	 * @param name
	 *            name of a script language
	 * @param fileExtension
	 *            script language script extension
	 * @param isModifi
	 *            determinate whether script language is validated as a new one
	 *            or modification of existing one.
	 * @return true when validation passed, false otherwise.
	 */
	protected void validateScript(final String name, final String fileExtension) {
		final StringBuilder errorMessage = new StringBuilder();

		if (StringUtil.isBlank(name)) {
			errorMessage.append(Messages.ScriptLanguageDialog_Cannot_Blank + CharactersConstants.NEW_LINE);
		}

		if (StringUtil.isBlank(fileExtension)) {
			errorMessage.append(Messages.ScriptLanguageDialog_Lang_Cannot_Blank + CharactersConstants.NEW_LINE);
		}

		if (scriptLanguages.containsKey(name) && !isModification) {
			errorMessage.append(Messages.ScriptLanguageDialog_Duplicated_Lang + CharactersConstants.NEW_LINE);
		}

		final Set<String> scriptLanguagesNames = scriptLanguages.entrySet().stream()
				.filter(entry -> Objects.equals(entry.getValue(), fileExtension)).map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		if (scriptLanguagesNames.size() > 0
				&& (!scriptLanguagesNames.contains(name) && !scriptLanguagesNames.contains(oldScriptLanguageName))) {
			errorMessage.append(Messages.ScriptLanguageDialog_Duplicated_File_Ext);
		}

		setMessage(errorMessage.toString());
		if (!StringUtil.isBlank(errorMessage.toString())) {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		} else {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
	}

	/**
	 * clear message.
	 */
	protected void clearMessage() {
		setMessage(CharactersConstants.EMPTY_STRING);
	}
	
	public String getScriptLanguageName() {
		return scriptLanguageName;
	}

	public void setScriptLanguageName(String scriptLanguageName) {
		this.scriptLanguageName = scriptLanguageName;
	}

	public String getScriptLanguageFileExtension() {
		return scriptLanguageFileExtension;
	}

	public void setScriptLanguageFileExtension(String scriptLanguageFileExtension) {
		this.scriptLanguageFileExtension = scriptLanguageFileExtension;
	}
}
