package org.apereo.cas.support.saml;

import org.apereo.cas.util.ResourceUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.shibboleth.idp.profile.spring.factory.BasicResourceCredentialFactoryBean;
import net.shibboleth.idp.profile.spring.factory.BasicX509CredentialFactoryBean;
import org.apache.commons.lang3.StringUtils;
import org.cryptacular.util.CertUtil;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.metadata.resolver.filter.impl.SignatureValidationFilter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.KeyInfoProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.DEREncodedKeyValueProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.DSAKeyValueProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.RSAKeyValueProvider;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * This is {@link SamlUtils}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@UtilityClass
public class SamlUtils {
    private static final int SAML_OBJECT_LOG_ASTERIXLINE_LENGTH = 80;
    private static final String NAMESPACE_URI = "http://www.w3.org/2000/xmlns/";

    /**
     * Read certificate x 509 certificate.
     *
     * @param resource the resource
     * @return the x 509 certificate
     */
    public static X509Certificate readCertificate(final Resource resource) {
        try (val in = resource.getInputStream()) {
            return CertUtil.readCertificate(in);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Error reading certificate " + resource, e);
        }
    }

    /**
     * Transform saml object into string without indenting the final string.
     *
     * @param configBean the config bean
     * @param samlObject the saml object
     * @return the string writer
     * @throws SamlException the saml exception
     */
    public static StringWriter transformSamlObject(final OpenSamlConfigBean configBean, final XMLObject samlObject) throws SamlException {
        return transformSamlObject(configBean, samlObject, false);
    }

    /**
     * Transform saml object t.
     *
     * @param <T>        the type parameter
     * @param configBean the config bean
     * @param xml        the xml
     * @param clazz      the clazz
     * @return the t
     */
    public static <T extends XMLObject> T transformSamlObject(final OpenSamlConfigBean configBean, final String xml,
                                                              final Class<T> clazz) {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            val document = configBean.getParserPool().parse(in);
            val root = document.getDocumentElement();

            val marshaller = configBean.getUnmarshallerFactory().getUnmarshaller(root);
            if (marshaller != null) {
                val result = marshaller.unmarshall(root);
                if (!clazz.isAssignableFrom(result.getClass())) {
                    throw new ClassCastException("Result [" + result
                        + " is of type " + result.getClass()
                        + " when we were expecting " + clazz);
                }
                return (T) result;
            }
        } catch (final Exception e) {
            throw new SamlException(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Transform saml object to String.
     *
     * @param configBean the config bean
     * @param samlObject the saml object
     * @param indent     the indent
     * @return the string
     * @throws SamlException the saml exception
     */
    public static StringWriter transformSamlObject(final OpenSamlConfigBean configBean, final XMLObject samlObject,
                                                   final boolean indent) throws SamlException {
        val writer = new StringWriter();
        try {
            val marshaller = configBean.getMarshallerFactory().getMarshaller(samlObject.getElementQName());
            if (marshaller != null) {
                val element = marshaller.marshall(samlObject);
                val domSource = new DOMSource(element);

                val result = new StreamResult(writer);
                val tf = TransformerFactory.newInstance();
                tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                val transformer = tf.newTransformer();

                if (indent) {
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                }
                transformer.transform(domSource, result);
            }
        } catch (final Exception e) {
            throw new SamlException(e.getMessage(), e);
        }
        return writer;
    }

    /**
     * Build signature validation filter if needed.
     *
     * @param signatureResourceLocation the signature resource location
     * @return the metadata filter
     * @throws Exception the exception
     */
    public static SignatureValidationFilter buildSignatureValidationFilter(final String signatureResourceLocation) throws Exception {
        val resource = ResourceUtils.getResourceFrom(signatureResourceLocation);
        return buildSignatureValidationFilter(resource);
    }

    /**
     * Build signature validation filter if needed.
     *
     * @param resourceLoader            the resource loader
     * @param signatureResourceLocation the signature resource location
     * @return the metadata filter
     */
    public static SignatureValidationFilter buildSignatureValidationFilter(final ResourceLoader resourceLoader,
                                                                           final String signatureResourceLocation) {
        try {
            val resource = resourceLoader.getResource(signatureResourceLocation);
            return buildSignatureValidationFilter(resource);
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Build signature validation filter if needed.
     *
     * @param signatureResourceLocation the signature resource location
     * @return the metadata filter
     * @throws Exception the exception
     */
    public static SignatureValidationFilter buildSignatureValidationFilter(final Resource signatureResourceLocation) throws Exception {
        if (!ResourceUtils.doesResourceExist(signatureResourceLocation)) {
            LOGGER.warn("Resource [{}] cannot be located", signatureResourceLocation);
            return null;
        }

        val keyInfoProviderList = new ArrayList<KeyInfoProvider>();
        keyInfoProviderList.add(new RSAKeyValueProvider());
        keyInfoProviderList.add(new DSAKeyValueProvider());
        keyInfoProviderList.add(new DEREncodedKeyValueProvider());
        keyInfoProviderList.add(new InlineX509DataProvider());

        LOGGER.debug("Attempting to resolve credentials from [{}]", signatureResourceLocation);
        val credential = buildCredentialForMetadataSignatureValidation(signatureResourceLocation);
        LOGGER.info("Successfully resolved credentials from [{}]", signatureResourceLocation);

        LOGGER.debug("Configuring credential resolver for key signature trust engine @ [{}]", credential.getCredentialType().getSimpleName());
        val resolver = new StaticCredentialResolver(credential);
        val keyInfoResolver = new BasicProviderKeyInfoCredentialResolver(keyInfoProviderList);
        val trustEngine = new ExplicitKeySignatureTrustEngine(resolver, keyInfoResolver);

        LOGGER.debug("Adding signature validation filter based on the configured trust engine");
        val signatureValidationFilter = new SignatureValidationFilter(trustEngine);
        signatureValidationFilter.setRequireSignedRoot(false);
        LOGGER.debug("Added metadata SignatureValidationFilter with signature from [{}]", signatureResourceLocation);
        return signatureValidationFilter;
    }

    /**
     * Build credential for metadata signature validation basic credential.
     *
     * @param resource the resource
     * @return the basic credential
     * @throws Exception the exception
     */
    public static BasicCredential buildCredentialForMetadataSignatureValidation(final Resource resource) throws Exception {
        try {
            val x509FactoryBean = new BasicX509CredentialFactoryBean();
            x509FactoryBean.setCertificateResource(resource);
            x509FactoryBean.afterPropertiesSet();
            return x509FactoryBean.getObject();
        } catch (final Exception e) {
            LOGGER.trace(e.getMessage(), e);

            LOGGER.debug("Credential cannot be extracted from [{}] via X.509. Treating it as a public key to locate credential...",
                resource);
            val credentialFactoryBean = new BasicResourceCredentialFactoryBean();
            credentialFactoryBean.setPublicKeyInfo(resource);
            credentialFactoryBean.afterPropertiesSet();
            return credentialFactoryBean.getObject();
        }
    }


    /**
     * Log saml object.
     *
     * @param configBean the config bean
     * @param samlObject the saml object
     * @return the string
     * @throws SamlException the saml exception
     */
    public static String logSamlObject(final OpenSamlConfigBean configBean, final XMLObject samlObject) throws SamlException {
        val repeat = StringUtils.repeat('*', SAML_OBJECT_LOG_ASTERIXLINE_LENGTH);
        LOGGER.debug(repeat);
        try (val writer = transformSamlObject(configBean, samlObject, true)) {
            LOGGER.debug("Logging [{}]\n\n{}\n\n", samlObject.getClass().getName(), writer);
            LOGGER.debug(repeat);
            return writer.toString();
        } catch (final Exception e) {
            throw new SamlException(e.getMessage(), e);
        }

    }
}
