<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" version="1.0" encoding="UTF-8"
        indent="yes" />

    <!-- Filter out undesired bundles -->
    <xsl:template match="bundle[@artifactId='cxf-karaf-commands']"></xsl:template>
    <xsl:template match="bundle[@artifactId='org.apache.karaf.http.core']"></xsl:template>
    <xsl:template match="bundle[@artifactId='org.apache.aries.spifly.dynamic.bundle']"></xsl:template>
    <xsl:template match="bundle[@groupId='org.eclipse.jetty.websocket']"></xsl:template>
    <xsl:template match="bundle[@artifactId='org.apache.karaf.scr.command']"></xsl:template>
    <xsl:template match="bundle[@artifactId='org.apache.felix.webconsole.plugins.ds']"></xsl:template>
    <xsl:template match="bundle[@artifactId='org.apache.aries.rsa.discovery.command']"></xsl:template>

    <!-- Copy the rest unachanged -->
    <xsl:template match="@* | node()">
        <xsl:copy>
          <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

</xsl:transform>
