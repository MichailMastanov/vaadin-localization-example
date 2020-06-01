package com.vaadin.example;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

/**
 * An application that demonstrate the configuration and usage of Vaadins
 * built-in localization (l10n) and internationalization (i18n) features.
 * <p>
 * In addition to this main class, there are two other important classes:
 * <p>
 * - {@link SimpleI18NProvider}, our translation implementation. It implements
 * the Vaadin {@link I18NProvider} interface. <br/>
 * - {@link ServiceInitListener}, that allows us to configure the language
 * whenever an user navigates to our app.
 * <p>
 * The actual translations can be found in the
 * <code>labelsbundle.properties</code> files under
 * <code>src/main/resources</code>. There is one properties file per language,
 * and a base file with translations that are the same for all languages.
 * <p>
 * Our translation provider class is automatically used by the framework,
 * allowing us to call {@link Component#getTranslation(String, Object...)}
 * whenever we need a translation.
 */
@SuppressWarnings("serial")
@Route("")
@CssImport("./styles/shared-styles.css")
public class MainView extends VerticalLayout implements LocaleChangeObserver {

	private final TextField nameField;
	private final Button greetingButton;
	private final Select<Locale> languageSelect;

	public MainView(@Autowired GreetService service, @Autowired I18NProvider i18NProvider) {

		/*
		 * Before we get this far in the code, Vaadin has parsed and applied a Locale.
		 *
		 * The list of available locales this particular app supports can be found in
		 * SimpleI18NProvider#getAvailableLocales().
		 *
		 * The deafult order of locale detection is as follows:
		 *
		 * 1. If there is no I18NProvider available, use the JVM (server) locale
		 *
		 * 2. Look at the browsers locale setting. If that locale is supported by our
		 * I18NProvider, use that.
		 *
		 * 3. Use the first item from I18NProvider#getAvailableLocales().
		 *
		 * However, we have added our own check with the cookie, that overrides this
		 * behaviour. See ServiceInitListener#initLanguage().
		 */

		System.out.println("Current locale is " + UI.getCurrent().getLocale());

		// Check if the user has a locale cookie. This is for information only, as the
		// UI init code has already found the cookie if it exists, and used it.
		final String cookieLang = findLocaleFromCookie();
		if ("".equals(cookieLang)) {

			add(new Span("No stored language preference found."));
			add(new Span("Defaulting to browser locale (if supported) or English."));
		} else {

			add(new Span("Locale was found from cookie: " + cookieLang));

			final Button clearCookieButton = new Button("[Clear language cookie and refresh page]",
					e -> clearLocalePreference());
			add(clearCookieButton);
		}

		// component for selecting another language
		languageSelect = new Select<>();
		languageSelect.setLabel("selectLanguage");
		languageSelect.setItems(i18NProvider.getProvidedLocales());
		languageSelect.setItemLabelGenerator(l -> getTranslation(l.getLanguage()));

		languageSelect.setValue(UI.getCurrent().getLocale());
		languageSelect.addValueChangeListener(event -> saveLocalePreference(event.getValue()));
		add(languageSelect);

		add(new Span("When you select a new language, your choice will be saved and re-used if you reload the page."));

		// Examples how to use translations.
		nameField = new TextField(getTranslation("yourName"));
		add(nameField);

		// The service can use the i18nProvider too.
		greetingButton = new Button(getTranslation("helloButton"),
				e -> Notification.show(service.greet(nameField.getValue(), getLocale())));
		greetingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		greetingButton.addClickShortcut(Key.ENTER);
		add(greetingButton);

		setAlignItems(Alignment.CENTER);
	}

	/**
	 * Util method that goes through the users cookies and finds a 'locale' cookie,
	 * if any.
	 */
	private String findLocaleFromCookie() {
		final Cookie[] cookies = VaadinRequest.getCurrent().getCookies();
		final Optional<String> cookie = Arrays.asList(cookies).stream().filter(c -> "locale".equals(c.getName()))
				.map(c -> c.getValue()).findAny();
		return cookie.orElse("");
	}

	/**
	 * Clears the users locale preference by deleting the 'locale' cookie.
	 */
	private void clearLocalePreference() {
		VaadinService.getCurrentResponse().addCookie(new Cookie("locale", null));
		getUI().get().getPage().reload();
	}

	/**
	 * Stores the users locale preference by creating a 'locale' cookie. This cookie
	 * will be read on app initialization by the class
	 * {@link ConfigureUIServiceInitListener}
	 */
	private void saveLocalePreference(Locale locale) {
		getUI().get().setLocale(locale);
		VaadinService.getCurrentResponse().addCookie(new Cookie("locale", locale.toLanguageTag()));
		Notification.show("Locale choice saved into cookie");
	}

	/*
	 * (non-javadoc, see method javadoc for more info.)
	 *
	 * Called whenever we call UI.setLocale(). In this listener method we should
	 * update all of our translated information.
	 *
	 * An alternative to updating each text is to reload the application; we stored
	 * the new locale in a cookie, so a reload would have the same effect. This way
	 * is cleaner for the than a reload, even though it requires us to do some
	 * manual work.
	 */
	@Override
	public void localeChange(LocaleChangeEvent localeChangeEvent) {

		greetingButton.setText(getTranslation("helloButton"));
		nameField.setLabel(getTranslation("yourName"));
		languageSelect.setLabel(getTranslation("selectLanguage"));
	}
}
