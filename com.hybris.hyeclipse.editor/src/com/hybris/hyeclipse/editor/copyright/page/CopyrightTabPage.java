package com.hybris.hyeclipse.editor.copyright.page;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

import com.hybris.hyeclipse.editor.copyright.constant.CopyrightConstants;
import com.hybris.hyeclipse.editor.copyright.manager.CopyrightManager;

/**
 * Implementation of {@link ICleanUpConfigurationUI} Save Actions tab with
 * copyright options
 */
public class CopyrightTabPage implements ICleanUpConfigurationUI {
	private static final int CLEANUP_COUNT = 2;
	private static final double VERTICAL_SPACING_MULTIPLIER = 1.5;
	private static final String ADD_COPYRIGHT = "Add Copyrights";
	private static final String NO_COPYRIGHTS = "";
	private static final String OVERRIDE_THE_COPYRIGHTS = "Override existing copyrights";
	private PixelConverter pixelConverter;
	private CleanUpOptions options;
	private final CopyrightManager copyrightManager = new CopyrightManager();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Composite createContents(final Composite parent) {
		final int numColumns = 2;

		if (pixelConverter == null) {
			pixelConverter = new PixelConverter(parent);
		}

		final SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.setFont(parent.getFont());

		final Composite scrollContainer = new Composite(sashForm, SWT.NONE);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		scrollContainer.setLayoutData(gridData);

		GridLayout layout = new GridLayout(numColumns, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		scrollContainer.setLayout(layout);

		final ScrolledComposite scroll = new ScrolledComposite(scrollContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);

		final Composite settingsContainer = new Composite(scroll, SWT.NONE);
		settingsContainer.setFont(sashForm.getFont());

		scroll.setContent(settingsContainer);

		settingsContainer.setLayout(new PageLayout(scroll, 400, 400));
		settingsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite settingsPane = new Composite(settingsContainer, SWT.NONE);
		settingsPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		layout = new GridLayout(numColumns, false);
		layout.verticalSpacing = (int) (VERTICAL_SPACING_MULTIPLIER
				* pixelConverter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING));
		layout.horizontalSpacing = pixelConverter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.marginHeight = pixelConverter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = pixelConverter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		settingsPane.setLayout(layout);
		doCreatePreferences(settingsPane);

		settingsContainer.setSize(settingsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		scroll.addControlListener(new ControlListener() {

			@Override
			public void controlMoved(final ControlEvent e) {
			}

			@Override
			public void controlResized(final ControlEvent e) {
				settingsContainer.setSize(settingsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		final Label sashHandle = new Label(scrollContainer, SWT.SEPARATOR | SWT.VERTICAL);
		gridData = new GridData(SWT.RIGHT, SWT.FILL, false, true);
		sashHandle.setLayoutData(gridData);

		return sashForm;
	}

	/**
	 * Creates the preferences for the tab page.
	 *
	 * @param composite
	 *            Composite to create in
	 */
	protected void doCreatePreferences(final Composite composite) {
		final Group group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(1, false));
		group.setText(ADD_COPYRIGHT);
		final Button updateCheckbox = new Button(group, SWT.CHECK);
		updateCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		updateCheckbox.setText(ADD_COPYRIGHT);
		updateCheckbox.setSelection(options.isEnabled(CopyrightConstants.CLEANUP_ADD_COPYRIGHTS));
		final Button overrideCheckbox = new Button(group, SWT.CHECK);
		overrideCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		overrideCheckbox.setText(OVERRIDE_THE_COPYRIGHTS);
		overrideCheckbox.setEnabled(updateCheckbox.getSelection());
		overrideCheckbox.setSelection(
				updateCheckbox.getSelection() && options.isEnabled(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS));

		updateCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				options.setOption(CopyrightConstants.CLEANUP_ADD_COPYRIGHTS,
						updateCheckbox.getSelection() ? CleanUpOptions.TRUE : CleanUpOptions.FALSE);
				overrideCheckbox.setEnabled(updateCheckbox.getSelection());
			}
		});
		overrideCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				options.setOption(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS,
						overrideCheckbox.getSelection() ? CleanUpOptions.TRUE : CleanUpOptions.FALSE);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCleanUpCount() {
		return CLEANUP_COUNT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPreview() {
		final StringBuilder builder = new StringBuilder();
		if (options.isEnabled(CopyrightConstants.CLEANUP_ADD_COPYRIGHTS)) {
			builder.append(copyrightManager.getCopyrightText());
		} else {
			builder.append(NO_COPYRIGHTS);
		}
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSelectedCleanUpCount() {
		int optionsCount = 0;
		if (options.isEnabled(CopyrightConstants.CLEANUP_ADD_COPYRIGHTS)) {
			optionsCount = 1;
		}
		if (options.isEnabled(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS)) {
			optionsCount = 2;
		}
		return optionsCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setOptions(final CleanUpOptions options) {
		this.options = options;
	}

	/**
	 * Layout used for the settings part. Makes sure to show scrollbars if
	 * necessary. The settings part needs to be layouted on resize.
	 */
	private static class PageLayout extends Layout {

		private final ScrolledComposite fContainer;
		private final int fMinimalWidth;
		private final int fMinimalHight;

		private PageLayout(final ScrolledComposite container, final int minimalWidth, final int minimalHight) {
			fContainer = container;
			fMinimalWidth = minimalWidth;
			fMinimalHight = minimalHight;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean force) {
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
				return new Point(wHint, hHint);
			}

			int x = fMinimalWidth;
			int y = fMinimalHight;
			final Control[] children = composite.getChildren();
			for (final Control child : children) {
				final Point size = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
				x = Math.max(x, size.x);
				y = Math.max(y, size.y);
			}

			final Rectangle area = fContainer.getClientArea();
			if (area.width > x) {
				fContainer.setExpandHorizontal(true);
			} else {
				fContainer.setExpandHorizontal(false);
			}

			if (area.height > y) {
				fContainer.setExpandVertical(true);
			} else {
				fContainer.setExpandVertical(false);
			}

			if (wHint != SWT.DEFAULT) {
				x = wHint;
			}
			if (hHint != SWT.DEFAULT) {
				y = hHint;
			}

			return new Point(x, y);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void layout(final Composite composite, final boolean force) {
			final Rectangle rect = composite.getClientArea();
			final Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i].setSize(rect.width, rect.height);
			}
		}

	}
}
