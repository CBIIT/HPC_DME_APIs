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
BEGIN
    RETURN to_timestamp($1, $3) < to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_greater_than(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) > to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_less_or_equal(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) <= to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_greater_or_equal(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) >= to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;
