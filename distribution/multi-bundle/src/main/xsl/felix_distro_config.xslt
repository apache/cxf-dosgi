<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:param name="Version"/>
  <xsl:param name="Offset"/>
  <xsl:template match="/">
org.ops4j.pax.web.session.timeout=30
org.osgi.framework.startlevel.beginning=200

<xsl:for-each select="//bundle[not(contains(@name,'cxf-karaf-commands'))]">
<xsl:sort select="@start-level" data-type="number"/>
<xsl:variable name="i" select="position() + count(//bundles/felix_deps) + $Offset"/>
felix.auto.start.<xsl:value-of select="$i"/>=file:dosgi_bundles/<xsl:value-of select="@name"/>
</xsl:for-each>

org.osgi.framework.system.packages=org.osgi.framework; version=1.5.0, \
 org.osgi.framework.launch; version=1.0.0, \
 org.osgi.framework.hooks.service; version=1.0.0, \
 org.osgi.framework.wiring; version=1.0.0 , \
 org.osgi.service.packageadmin; version=1.2.0, \
 org.osgi.service.startlevel; version=1.1.0, \
 org.osgi.service.url; version=1.0.0, \
 org.osgi.util.tracker; version=1.4.0, \
 org.apache.karaf.jaas.boot; version=2.2.9, \
 org.apache.karaf.version; version=2.2.9, \
 javax.annotation.processing, \
 javax.crypto, \
 javax.crypto.spec, \
 javax.imageio, \
 javax.imageio.stream, \
 javax.lang.model, \
 javax.lang.model.element, \
 javax.lang.model.type, \
 javax.lang.model.util, \
 javax.naming, \
 javax.xml.bind.annotation, \
 javax.xml.datatype, \
 javax.xml.parsers, \
 javax.xml.namespace, \
 javax.xml.transform, \
 javax.xml.transform.dom, \
 javax.xml.transform.sax, \
 javax.xml.transform.stream, \
 javax.xml.validation, \
 javax.xml.xpath, \
 javax.management, \
 javax.management.modelmbean, \
 javax.management.remote, \
 javax.naming.directory, \
 javax.naming.spi, \
 javax.net, \
 javax.net.ssl, \
 javax.security.auth, \
 javax.security.auth.callback, \
 javax.security.auth.login, \
 javax.security.auth.spi, \
 javax.security.auth.x500, \
 javax.security.cert, \
 javax.sql, \
 javax.swing, \
 javax.swing.border, \
 javax.swing.tree, \
 javax.tools, \
 javax.transaction; javax.transaction.xa; partial=true; mandatory:=partial, \
 javax.xml.transform.stax, \
 javax.wsdl, \
 javax.wsdl.extensions, \
 org.ietf.jgss, \
 org.xml.sax, \
 org.xml.sax.ext, \
 org.xml.sax.helpers, \
 org.w3c.dom, \
 org.w3c.dom.bootstrap, \
 org.w3c.dom.ls

  </xsl:template>
</xsl:transform>
