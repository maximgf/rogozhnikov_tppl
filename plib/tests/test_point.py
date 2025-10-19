import pytest
import json

from plib import Point

@pytest.fixture
def points():
    return Point(0, 0), Point(2, 2)

class TestPoint:

    def test_creation(self):
        p = Point(1, 2)
        assert p.x == 1 and p.y == 2

        with pytest.raises(TypeError):
            Point(1.5, 1.5)
        with pytest.raises(TypeError):
            Point(1, 1.5)
        with pytest.raises(TypeError):
            Point(1.5, 1)

    def test_add(self, points):
        p1, p2 = points
        assert p2 + p1 == Point(2, 2)
        assert p2 + p2 == Point(4, 4)
    
    def test_iadd(self, points):
        p1, p2 = points
        p_mutable = Point(1, 1)
        p_mutable += p2
        assert p_mutable == Point(3, 3)
        p_iadded = Point(5, 5)
        result = p_iadded.__iadd__(p1)
        assert result == Point(5, 5)

    def test_sub(self, points):
        p1, p2 = points
        assert p2 - p1 == Point(2, 2)
        assert p1 - p2 == -Point(2, 2)
        assert p2 - p2 == Point(0, 0)
    
    def test_neg(self, points):
        p1, p2 = points
        assert -p2 == Point(-2, -2)
        assert -p1 == Point(0, 0)

    def test_eq(self):
        p1 = Point(1, 2)
        p2 = Point(1, 2)
        p3 = Point(2, 1)
        
        assert p1 == p2
        assert not (p1 == p3)

        with pytest.raises(NotImplementedError):
            p1 == (1, 2)

    def test_distance_to(self):
        p1 = Point(0, 0)
        p2 = Point(2, 0)
        assert p1.to(p2) == 2

    @pytest.mark.parametrize(
            "p1, p2, distance",
            [(Point(0, 0), Point(0, 10), 10),
             (Point(0, 0), Point(10, 0), 10),
             (Point(0, 0), Point(1, 1), 1.414)]
    )
    def test_distance_all_axis(self, p1, p2, distance):
        assert p1.to(p2) == pytest.approx(distance, 0.001)

    def test_is_center(self):
        p_center = Point(0, 0)
        p_not_center = Point(1, 0)
        
        assert p_center.is_center() is True
        assert p_not_center.is_center() is False

    def test_to_from_json(self):
        original_point = Point(5, -10)
        json_string = original_point.to_json()
        
        data = json.loads(json_string)
        assert data == {"x": 5, "y": -10}

        new_point = Point.from_json(json_string)
        assert original_point == new_point
        assert new_point.x == 5
        assert new_point.y == -10
        assert isinstance(new_point, Point)

    def test_str_repr(self):
        p = Point(3, 4)
        expected_str = "Point(3, 4)"
        
        assert str(p) == expected_str
        assert repr(p) == expected_str