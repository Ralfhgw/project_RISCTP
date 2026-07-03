from typing import List, Literal, Union, Self, Final

# A type alias for the allowed argument types: Strings or nested Exp objects
ExpArg = Union[str, 'Exp']

class Exp:
    def __init__(
        self, 
        tag: Literal[
            "var", "fun", "true", "false", "pred", "not", 
            "and", "or", "imp", "iff", "forall", "exists"
        ], 
        args: List[ExpArg] #| None = None
    ) -> None:
        """
        Initializes an Abstract Syntax Tree node for a formal expression.
        
        :param tag: The operator or quantifier type.
        :param args: A list of arguments (either strings or other Exp objects).
        """
        self.tag: Final[str] = tag
        self.args: Final[List[ExpArg]] = args if args is not None else []

    def __str__(self) -> str:
        """
        Returns the string representation in the format tag[] or tag["arg1",...]
        Strings are wrapped in double quotes; Exp objects are called recursively.
        """
        if not self.args:
            return f"{self.tag}[]"
        
        formatted_args: List[str] = []
        for arg in self.args:
            if isinstance(arg, str):
                formatted_args.append(f'"{arg}"')
            else:
                formatted_args.append(str(arg))
        
        return f"{self.tag}[{','.join(formatted_args)}]"

# --- Example Usage ---
if __name__ == "__main__":
    # Representing: ∀x (P(x) → Q(x))
    p_x = Exp(tag="pred", args=["P", Exp(tag="var", args=["x"])])
    q_x = Exp(tag="pred", args=["Q", Exp(tag="var", args=["x"])])
    implication = Exp(tag="imp", args=[p_x, q_x])
    forall_stmt = Exp(tag="forall", args=["x", implication])

    print(forall_stmt)
    # Output: forall["x",imp[pred["P",var["x"]],pred["Q",var["x"]]]]