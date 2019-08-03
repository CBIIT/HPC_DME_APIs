--
-- hpc_hierarchical_metadata.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--

CREATE OR REPLACE FUNCTION timestamp_less_than(text, text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value TEXT;
DECLARE value TEXT;
DECLARE format TEXT;
BEGIN
    attr_value = $1::TEXT;
    value = $2::TEXT;
    format = $3::TEXT;
    RETURN to_timestamp(attr_value, format) < value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_greater_than(text, text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value TEXT;
DECLARE value TEXT;
DECLARE format TEXT;
BEGIN
    attr_value = $1::TEXT;
    value = $2::TEXT;
    format = $3::TEXT;
    RETURN to_timestamp(attr_value, format) > value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;
