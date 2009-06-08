<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:param name="Version"/>
  <xsl:variable name = "Basename">.dir/apache-cxf-dosgi-ri-<xsl:value-of select="$Version"/>/dosgi_bundles/</xsl:variable>
  <xsl:template match="/">
<xsl:for-each select="//bundles/bundle">../apache-cxf-dosgi-ri-<xsl:value-of select="$Version"/>/dosgi_bundles/<xsl:value-of select="substring-after(text(), $Basename)"/><xsl:value-of select="string('@start, ')"/></xsl:for-each>
  </xsl:template>
</xsl:transform>

