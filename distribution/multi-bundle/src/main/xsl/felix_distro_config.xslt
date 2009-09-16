<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:param name="Version"/>
  <xsl:variable name = "Basename">.dir/apache-cxf-dosgi-ri-<xsl:value-of select="$Version"/>/dosgi_bundles/</xsl:variable>
  <xsl:template match="/">
org.ops4j.pax.web.session.timeout=30
org.osgi.framework.startlevel.beginning=<xsl:value-of select="count(//bundles/felix_deps) + count(//bundles/bundle) + 2"/>
felix.auto.start.2=http://repo2.maven.org/maven2/org/osgi/org.osgi.compendium/4.1.0/org.osgi.compendium-4.1.0.jar
    <xsl:for-each select="//bundles/bundle">
      <xsl:variable name="i" select="position() + count(//bundles/felix_deps) + 2"/>
felix.auto.start.<xsl:value-of select="$i"/>=file:apache-cxf-dosgi-ri-<xsl:value-of select="$Version"/>/dosgi_bundles/<xsl:value-of select="substring-after(text(), $Basename)"/>
    </xsl:for-each>
  </xsl:template>
</xsl:transform>

