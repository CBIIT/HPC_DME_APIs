DECLARE
    cnt           NUMBER(10);
    meta_cnt        NUMBER(10);
    del_meta_id         NUMBER(10);
    CURSOR cur
        IS
        SELECT meta_attr_name, coll_id, coll_name, count(*) from (
                                                                     SELECT cast(c.coll_name as varchar2(1000)) as    coll_name,
                                                                            cast(a.meta_attr_name as varchar2(1000))  meta_attr_name,
                                                                            cast(a.meta_attr_value as varchar2(2700)) meta_attr_value,
                                                                            c.coll_id,
                                                                            b.meta_id,
                                                                            cast(c.create_ts as varchar2(32))   as    create_ts,
                                                                            count(*)
                                                                     FROM r_meta_main a,
                                                                          r_objt_metamap b,
                                                                          r_coll_main c
                                                                     WHERE a.meta_id = b.meta_id
                                                                       AND b.object_id = c.coll_id
                                                                     GROUP BY cast(c.coll_name as varchar2(1000)),
                                                                              cast(a.meta_attr_name as varchar2(1000)),
                                                                              cast(a.meta_attr_value as varchar2(2700)),
                                                                              c.coll_id,
                                                                              b.meta_id,
                                                                              cast(c.create_ts as varchar2(32))
                                                                 )
        group by meta_attr_name, coll_id, coll_name having count(*)>1;

BEGIN
    cnt := 0;
    FOR rec IN cur
        LOOP
            select count(main.meta_id) into meta_cnt from R_OBJT_METAMAP map, r_meta_main main
            where map.OBJECT_ID=rec.COLL_ID and map.META_ID=main.META_ID
              and main.META_ATTR_NAME=rec.meta_attr_name;
            IF meta_cnt > 1 THEN
                select max(main.META_ID) into del_meta_id from R_OBJT_METAMAP map, r_meta_main main
                where map.OBJECT_ID=rec.COLL_ID and map.META_ID=main.META_ID
                and main.META_ATTR_NAME=rec.meta_attr_name;
                delete from R_OBJT_METAMAP where OBJECT_ID=rec.COLL_ID and META_ID=del_meta_id;
                dbms_output.put_line('Collection ID: ' || rec.COLL_ID || ', attr_name: ' || rec.meta_attr_name || ', meta_id: ' || del_meta_id);
                cnt := cnt + 1;
            END IF;
        END LOOP;
    dbms_output.put_line('Total records removed: ' || cnt);

END;