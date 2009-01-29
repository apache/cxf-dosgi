<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:param name="TargetDir"/>
  <xsl:template match="/">
org.osgi.framework.startlevel=<xsl:value-of select="count(//bundles/felix_deps) + count(//bundles/bundle) + 1"/>
    <xsl:for-each select="//bundles/felix_deps">
      <xsl:variable name="i" select="position() + 1"/>
felix.auto.start.<xsl:value-of select="$i"/>=file:/<xsl:value-of select="$TargetDir"/><xsl:value-of select="text()"/>
    </xsl:for-each>
    <xsl:for-each select="//bundles/bundle">
      <xsl:variable name="i" select="position() + count(//bundles/felix_deps) + 1"/>
felix.auto.start.<xsl:value-of select="$i"/>=file:/<xsl:value-of select="$TargetDir"/><xsl:value-of select="text()"/>
    </xsl:for-each>
  </xsl:template>
</xsl:transform>

