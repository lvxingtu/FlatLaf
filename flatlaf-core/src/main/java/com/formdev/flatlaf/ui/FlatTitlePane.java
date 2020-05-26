/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.util.UIScale;

/**
 * Provides the Flat LaF title bar.
 *
 * @uiDefault TitlePane.background							Color
 * @uiDefault TitlePane.inactiveBackground					Color
 * @uiDefault TitlePane.foreground							Color
 * @uiDefault TitlePane.inactiveForeground					Color
 * @uiDefault TitlePane.closeIcon							Icon
 * @uiDefault TitlePane.iconifyIcon							Icon
 * @uiDefault TitlePane.maximizeIcon						Icon
 * @uiDefault TitlePane.minimizeIcon						Icon
 *
 * @author Karl Tauber
 */
class FlatTitlePane
	extends JComponent
{
	private final Color activeBackground = UIManager.getColor( "TitlePane.background" );
	private final Color inactiveBackground = UIManager.getColor( "TitlePane.inactiveBackground" );
	private final Color activeForeground = UIManager.getColor( "TitlePane.foreground" );
	private final Color inactiveForeground = UIManager.getColor( "TitlePane.inactiveForeground" );

	private final JRootPane rootPane;

	private JLabel titleLabel;
	private JPanel buttonPanel;
	private JButton iconifyButton;
	private JButton maximizeButton;
	private JButton restoreButton;
	private JButton closeButton;

	private final Handler handler = new Handler();
	private Window window;

	FlatTitlePane( JRootPane rootPane ) {
		this.rootPane = rootPane;

		addSubComponents();
		activeChanged( true );

		addMouseListener( handler );
	}

	private void addSubComponents() {
		titleLabel = new JLabel();
		titleLabel.setBorder( new FlatEmptyBorder( UIManager.getInsets( "TitlePane.titleMargins" ) ) );

		createButtons();

		setLayout( new BorderLayout( UIScale.scale( 4 ), 0 ) );
		add( titleLabel, BorderLayout.CENTER );
		add( buttonPanel, BorderLayout.EAST );
	}

	private void createButtons() {
		iconifyButton = createButton( "TitlePane.iconifyIcon", "Iconify", e -> iconify() );
		maximizeButton = createButton( "TitlePane.maximizeIcon", "Maximize", e -> maximize() );
		restoreButton = createButton( "TitlePane.minimizeIcon", "Restore", e -> restore() );
		closeButton = createButton( "TitlePane.closeIcon", "Close", e -> close() );

		buttonPanel = new JPanel();
		buttonPanel.setOpaque( false );
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.X_AXIS ) );
		if( rootPane.getWindowDecorationStyle() == JRootPane.FRAME ) {
			// JRootPane.FRAME works only for frames (and not for dialogs)
			// but at this time the owner window type is unknown (not yet added)
			// so we add the iconify/maximize/restore buttons and they are hidden
			// later in frameStateChanged(), which is invoked from addNotify()

			restoreButton.setVisible( false );

			buttonPanel.add( iconifyButton );
			buttonPanel.add( maximizeButton );
			buttonPanel.add( restoreButton );
		}
		buttonPanel.add( closeButton );
	}

	private JButton createButton( String iconKey, String accessibleName, ActionListener action ) {
		JButton button = new JButton( UIManager.getIcon( iconKey ) );
		button.setFocusable( false );
		button.setContentAreaFilled( false );
		button.setBorder( BorderFactory.createEmptyBorder() );
		button.putClientProperty( AccessibleContext.ACCESSIBLE_NAME_PROPERTY, accessibleName );
		button.addActionListener( action );
		return button;
	}

	private void activeChanged( boolean active ) {
		setBackground( active ? activeBackground : inactiveBackground );
		titleLabel.setForeground( FlatUIUtils.nonUIResource( active ? activeForeground : inactiveForeground ) );
	}

	private void frameStateChanged() {
		if( window == null || rootPane.getWindowDecorationStyle() != JRootPane.FRAME )
			return;

		if( window instanceof Frame ) {
			Frame frame = (Frame) window;
			boolean resizable = frame.isResizable();
			boolean maximized = ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);

			iconifyButton.setVisible( true );
			maximizeButton.setVisible( resizable && !maximized );
			restoreButton.setVisible( resizable && maximized );
		} else {
			// hide buttons because they are only supported in frames
			iconifyButton.setVisible( false );
			maximizeButton.setVisible( false );
			restoreButton.setVisible( false );

			revalidate();
			repaint();
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();

		uninstallWindowListeners();

		window = SwingUtilities.getWindowAncestor( this );
		if( window != null ) {
			frameStateChanged();
			activeChanged( window.isActive() );
			titleLabel.setText( getWindowTitle() );
			installWindowListeners();
		}
	}

	@Override
	public void removeNotify() {
		super.removeNotify();

		uninstallWindowListeners();
		window = null;
	}

	private String getWindowTitle() {
		if( window instanceof Frame )
			return ((Frame)window).getTitle();
		if( window instanceof Dialog )
			return ((Dialog)window).getTitle();
		return null;
	}

	private void installWindowListeners() {
		if( window == null )
			return;

		window.addPropertyChangeListener( handler );
		window.addWindowListener( handler );
		window.addWindowStateListener( handler );
	}

	private void uninstallWindowListeners() {
		if( window == null )
			return;

		window.removePropertyChangeListener( handler );
		window.removeWindowListener( handler );
		window.removeWindowStateListener( handler );
	}

	@Override
	protected void paintComponent( Graphics g ) {
		g.setColor( getBackground() );
		g.fillRect( 0, 0, getWidth(), getHeight() );
	}

	private void iconify() {
		if( window instanceof Frame ) {
			Frame frame = (Frame) window;
			frame.setExtendedState( frame.getExtendedState() | Frame.ICONIFIED );
		}
	}

	private void maximize() {
		if( window instanceof Frame ) {
			Frame frame = (Frame) window;
			frame.setExtendedState( frame.getExtendedState() | Frame.MAXIMIZED_BOTH );
		}
	}

	private void restore() {
		if( window instanceof Frame ) {
			Frame frame = (Frame) window;
			int state = frame.getExtendedState();
			frame.setExtendedState( ((state & Frame.ICONIFIED) != 0)
				? (state & ~Frame.ICONIFIED)
				: (state & ~Frame.MAXIMIZED_BOTH) );
		}
	}

	private void close() {
		if( window != null )
			window.dispatchEvent( new WindowEvent( window, WindowEvent.WINDOW_CLOSING ) );
	}

	//---- class Handler ------------------------------------------------------

	private class Handler
		extends WindowAdapter
		implements PropertyChangeListener, MouseListener
	{
		//---- interface PropertyChangeListener ----

		@Override
		public void propertyChange( PropertyChangeEvent e ) {
			switch( e.getPropertyName() ) {
				case "title":
					titleLabel.setText( getWindowTitle() );
					break;

				case "resizable":
					if( window instanceof Frame )
						frameStateChanged();
					break;
			}
		}

		//---- interface WindowListener ----

		@Override
		public void windowActivated( WindowEvent e ) {
			activeChanged( true );
		}

		@Override
		public void windowDeactivated( WindowEvent e ) {
			activeChanged( false );
		}

		@Override
		public void windowStateChanged( WindowEvent e ) {
			frameStateChanged();
		}

		//---- interface MouseListener ----

		@Override
		public void mouseClicked( MouseEvent e ) {
			if( e.getClickCount() == 2 &&
				SwingUtilities.isLeftMouseButton( e ) &&
				window instanceof Frame &&
				((Frame)window).isResizable() )
			{
				// maximize/restore on double-click
				Frame frame = (Frame) window;
				int state = frame.getExtendedState();
				frame.setExtendedState( ((state & Frame.MAXIMIZED_BOTH) != 0)
					? (state & ~Frame.MAXIMIZED_BOTH)
					: (state | Frame.MAXIMIZED_BOTH) );
			}
		}

		@Override public void mousePressed( MouseEvent e ) {}
		@Override public void mouseReleased( MouseEvent e ) {}
		@Override public void mouseEntered( MouseEvent e ) {}
		@Override public void mouseExited( MouseEvent e ) {}
	}
}
