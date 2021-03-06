/*
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.notify;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import dorkbox.util.ActionHandler;
import dorkbox.util.LocationResolver;
import dorkbox.util.SwingUtil;

/**
 * Popup notification messages, similar to the popular "Growl" notification system on macosx, that display in the corner of the monitor.
 * </p>
 * They can follow the mouse (if the screen is unspecified), and have a variety of features, such as "shaking" to draw attention,
 * animating upon movement (for collating w/ multiple in a single location), and automatically hiding after a set duration.
 * </p>
 * These notifications are for a single screen only, and cannot be anchored to an application.
 *
 * <pre>
 * {@code
 * Notify.create()
 *      .title("Title Text")
 *      .text("Hello World!")
 *      .useDarkStyle()
 *      .showWarning();
 * }
 * </pre>
 */
public final
class Notify {

    /**
     * Location of the dialog image resources. By default they must be in the 'resources' directory relative to the application
     */
    public static String IMAGE_PATH = "resources";

    private static Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>(4);
    private static Map<String, ImageIcon> imageIconCache = new HashMap<String, ImageIcon>(4);

    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "2.16";
    }

    /**
     * Builder pattern to create the notification.
     */
    public static
    Notify create() {
        return new Notify();
    }

    /**
     * Permits one to override the default images for the dialogs. This is NOT thread safe, and must be performed BEFORE showing a
     * notification.
     * <p>
     * The image names are as follows:
     * <p>
     * 'dialog-confirm.png' 'dialog-error.png' 'dialog-information.png' 'dialog-warning.png'
     *
     * @param imageName  the name of the image, either your own if you want want it cached, or one of the above.
     * @param image  the BufferedImage that you want to cache.
     */
    public static
    void setImagePath(String imageName, BufferedImage image) {
        if (imageCache.containsKey(imageName)) {
            throw new RuntimeException("Unable to set an image that already has been set. This action must be done as soon as possible.");
        }

        imageCache.put(imageName, image);
    }

    private static
    BufferedImage getImage(String imageName) {
        BufferedImage bufferedImage = imageCache.get(imageName);
        InputStream resourceAsStream = null;
        try {
            if (bufferedImage == null) {
                String name = IMAGE_PATH + File.separatorChar + imageName;

                resourceAsStream = LocationResolver.getResourceAsStream(name);

                bufferedImage = ImageIO.read(resourceAsStream);
                imageCache.put(imageName, bufferedImage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bufferedImage;
    }


    String title;
    String text;
    Pos position = Pos.BOTTOM_RIGHT;
    int hideAfterDurationInMillis = 0;
    boolean hideCloseButton;
    boolean isDark = false;
    int screenNumber = Short.MIN_VALUE;
    private Image graphic;
    private ActionHandler<Notify> onAction;
    private NotifyPopup notifyPopup;
    private String name;
    private int shakeDurationInMillis = 0;
    private int shakeAmplitude = 0;

    private
    Notify() {
    }

    /**
     * Specifies the main text
     */
    public
    Notify text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Specifies the title
     */
    public
    Notify title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Specifies the graphic
     */
    public
    Notify graphic(Image graphic) {
        this.graphic = graphic;
        return this;
    }

    /**
     * Specifies the position of the notification on screen, by default it is {@link Pos#BOTTOM_RIGHT bottom-right}.
     */
    public
    Notify position(Pos position) {
        this.position = position;
        return this;
    }

    /**
     * Specifies the duration that the notification should show, after which it will be hidden. 0 means to show forever. By default it
     * will show forever
     */
    public
    Notify hideAfter(int durationInMillis) {
        if (durationInMillis < 0) {
            durationInMillis = 0;
        }
        this.hideAfterDurationInMillis = durationInMillis;
        return this;
    }

    /**
     * Specifies what to do when the user clicks on the notification (in addition o the notification hiding, which happens whenever the
     * notification is clicked on). This does not apply when clicking on the "close" button
     */
    public
    Notify onAction(ActionHandler<Notify> onAction) {
        this.onAction = onAction;
        return this;
    }

    /**
     * Specifies that the notification should use the built-in dark styling, rather than the default, light-gray notification style.
     */
    public
    Notify darkStyle() {
        isDark = true;
        return this;
    }

    /**
     * Specify that the close button in the top-right corner of the notification should not be shown.
     */
    public
    Notify hideCloseButton() {
        this.hideCloseButton = true;
        return this;
    }

    /**
     * Shows the notification with the built-in 'warning' graphic.
     */
    public
    void showWarning() {
        name = "dialog-warning.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification with the built-in 'information' graphic.
     */
    public
    void showInformation() {
        name = "dialog-information.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification with the built-in 'error' graphic.
     */
    public
    void showError() {
        name = "dialog-error.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification with the built-in 'confirm' graphic.
     */
    public
    void showConfirm() {
        name = "dialog-confirm.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification
     */
    public
    void show() {
        // must be done in the swing EDT
        //noinspection Convert2Lambda
        SwingUtil.invokeAndWait(new Runnable() {
            @Override
            public
            void run() {
                final Notify notify = Notify.this;
                final Image graphic = notify.graphic;

                if (graphic == null) {
                    notifyPopup = new NotifyPopup(notify, null, null);
                }
                else {
                    // we ONLY cache our own icons
                    ImageIcon imageIcon;
                    if (name != null) {
                        imageIcon = imageIconCache.get(name);
                        if (imageIcon == null) {
                            imageIcon = new ImageIcon(graphic);
                            imageIconCache.put(name, imageIcon);
                        }
                    }
                    else {
                        imageIcon = new ImageIcon(graphic);
                    }

                    notifyPopup = new NotifyPopup(notify, graphic, imageIcon);
                }

                notifyPopup.setVisible(true);

                if (shakeDurationInMillis > 0) {
                    notifyPopup.shake(notify.shakeDurationInMillis, notify.shakeAmplitude);
                }
            }
        });
    }

    /**
     * "shakes" the notification, to bring user attention to it.
     *
     * @param durationInMillis now long it will shake
     * @param amplitude a measure of how much it needs to shake. 4 is a small amount of shaking, 10 is a lot.
     */
    public
    Notify shake(final int durationInMillis, final int amplitude) {
        this.shakeDurationInMillis = durationInMillis;
        this.shakeAmplitude = amplitude;

        if (notifyPopup != null) {
            // must be done in the swing EDT
            //noinspection Convert2Lambda
            SwingUtil.invokeLater(new Runnable() {
                @Override
                public
                void run() {
                    notifyPopup.shake(durationInMillis, amplitude);
                }
            });
        }

        return this;
    }

    /**
     * Closes the notification. Particularly useful if it's an "infinite" duration notification.
     */
    public
    void close() {
        if (notifyPopup == null) {
            throw new NullPointerException("NotifyPopup");
        }

        // must be done in the swing EDT
        //noinspection Convert2Lambda
        SwingUtil.invokeLater(new Runnable() {
            @Override
            public
            void run() {
                notifyPopup.close();
            }
        });
    }

    /**
     * Specifies which screen to display on. If <0, it will show on screen 0. If > max-screens, it will show on the last screen.
     */
    public
    Notify setScreen(final int screenNumber) {
        this.screenNumber = screenNumber;
        return this;
    }

    void onClick() {
        if(onAction != null) {
            onAction.handle(this);
        }
    }

    void onClose() {
        notifyPopup = null;
        graphic = null;
    }
}

