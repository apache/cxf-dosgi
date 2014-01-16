<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
<xsl:template match="/">
# equinox config
org.ops4j.pax.web.session.timeout=30

osgi.bundles=org.eclipse.osgi.services@start, \
../plugins/org.eclipse.equinox.console_1.0.0.v20120522-1841.jar@start, \
../plugins/org.apache.felix.gogo.shell_0.8.0.v201110170705.jar@start, \
../plugins/org.apache.felix.gogo.command_0.8.0.v201108120515.jar@start, \
../plugins/org.apache.felix.gogo.runtime_0.8.0.v201108120515.jar@start, \
<xsl:for-each select="//bundle"><xsl:sort select="@start-level" data-type="number" order="ascending"/>../dosgi_bundles/<xsl:value-of select="@name"/>@start,\
</xsl:for-each>
  </xsl:template>
</xsl:transform>

