<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:param name="TargetDir"/>
  <xsl:template match="/">
org.ops4j.pax.web.session.timeout=30

osgi.bundles=org.eclipse.osgi.services@start, \
  <xsl:for-each select="//bundles/bundle"><xsl:value-of select="$TargetDir"/><xsl:value-of select="text()"/><xsl:value-of select="string('@start, ')"/></xsl:for-each>
  </xsl:template>
</xsl:transform>

