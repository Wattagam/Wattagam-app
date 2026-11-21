-- 팀 확정 장소 이름은 '청룡탕'이다 (V3의 '청룡연못'을 바꾼다). 좌표·반경·순서는 그대로 둔다.
UPDATE places
SET name = '청룡탕',
    visit_point_name = '청룡탕 중심점'
WHERE id = 3;

UPDATE places
SET directions = '청룡탕에서 언덕길을 따라 올라가면 310관 앞 광장이에요.'
WHERE id = 1;
