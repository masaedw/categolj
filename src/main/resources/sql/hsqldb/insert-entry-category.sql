INSERT INTO EntryCategory (entry_id, category_id) 
VALUES (
     /*entry-id*/1,
/*IF name == null*/
     /*category-id*/1
/*END*/
/*IF name != null*/
     (SELECT id FROM Category WHERE name = /*name*/'Hoge')
/*END*/
)
